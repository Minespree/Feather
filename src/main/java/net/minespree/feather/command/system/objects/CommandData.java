package net.minespree.feather.command.system.objects;

import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.minespree.feather.FeatherPlugin;
import net.minespree.feather.command.system.CommandManager;
import net.minespree.feather.command.system.annotation.Command;
import net.minespree.feather.command.system.annotation.HelpFilter;
import net.minespree.feather.player.NetworkPlayer;
import net.minespree.feather.player.rank.Rank;
import net.minespree.wizard.util.Chat;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import static net.minespree.feather.util.ArrayUtil.concat;

public class CommandData {
    private static final net.md_5.bungee.api.ChatColor REQUIRED_COLOUR = net.md_5.bungee.api.ChatColor.GREEN;

    private static final net.md_5.bungee.api.ChatColor OPTIONAL_COLOUR = net.md_5.bungee.api.ChatColor.GOLD;

    private final String[] names;

    private final String[] flags;

    private final boolean async;

    private final boolean requirePlayer;

    private final Rank requiredRank;

    private final ParamData[] parameters;

    private final Object instance;

    private final Method method;

    private final int requiredCount;

    private final BaseComponent[] usage;

    private final Set<String> filters;
    private final long cooldown;
    private boolean hideFromHelp;

    public CommandData(Method method, Command commandAnnotation, ParamData[] parameters) {
        this(null, method, commandAnnotation, parameters);
    }

    public CommandData(Object instance, Method method, Command commandAnnotation, ParamData[] parameters) {
        this.names = commandAnnotation.names();
        this.flags = commandAnnotation.flags();
        this.requiredRank = commandAnnotation.requiredRank();
        this.parameters = parameters;
        this.requiredCount = (int) Arrays.stream(this.parameters).filter(ParamData::isRequired).count();
        this.async = commandAnnotation.async();
        this.instance = instance;
        this.method = method;
        this.requirePlayer = Player.class.isAssignableFrom(method.getParameterTypes()[0]);
        this.hideFromHelp = commandAnnotation.hideFromHelp();

        Class<?> cls = this.method.getDeclaringClass();
        String pluginName = JavaPlugin.getProvidingPlugin(cls).getName();
        if (cls.isAnnotationPresent(HelpFilter.class)) {
            this.filters = new HashSet<>();
            this.filters.add(pluginName);
            Collections.addAll(this.filters, cls.getAnnotation(HelpFilter.class).value());
        } else {
            this.filters = Collections.singleton(pluginName.toLowerCase());
        }

        this.cooldown = commandAnnotation.cooldown();

        BaseComponent[] command = TextComponent.fromLegacyText("/" + this.getName() + " ");
        BaseComponent[][] parametersDescriptions = new BaseComponent[this.getParameters().length][];
        ParamData[] params = this.getParameters();
        for (int i = 0; i < params.length; i++) {
            ParamData param = params[i];
            ComponentBuilder parameter = new ComponentBuilder((param.isRequired() ? '<' : '[') + param.getName() + (param.isRequired() ? '>' : ']') + " ");
            parameter.color(param.isRequired() ? REQUIRED_COLOUR : OPTIONAL_COLOUR);
            ComponentBuilder builder = new ComponentBuilder(net.md_5.bungee.api.ChatColor.GREEN + "Description:\n")
                    .append(param.getDescription() + "\n").color(net.md_5.bungee.api.ChatColor.GRAY);

            parameter.event(new HoverEvent(HoverEvent.Action.SHOW_TEXT, builder.create()));
            parametersDescriptions[i] = parameter.create();
        }
        BaseComponent[] rankText = TextComponent.fromLegacyText(Chat.GRAY + "(" + this.getRequiredRank().getColor() + this.getRequiredRank().name() + Chat.GRAY + ")");

        this.usage = concat(BaseComponent[].class, command, concat(BaseComponent[].class, parametersDescriptions), rankText);
    }

    public String getName() {
        return (names[0]);
    }

    public ParamData[] getParameters() {
        return this.parameters;
    }

    public Rank getRequiredRank() {
        return this.requiredRank;
    }

    public boolean canAccess(Player player) {
        return NetworkPlayer.of(player).getRank().has(requiredRank) || player.isOp();
    }

    public String getUsageString() {
        return (getUsageString(getName()));
    }

    public String getUsageString(String aliasUsed) {
        StringBuilder stringBuilder = new StringBuilder();

        for (ParamData paramHelp : getParameters()) {
            boolean needed = paramHelp.isRequired();
            stringBuilder.append(needed ? "<" : "[").append(paramHelp.getName());
            stringBuilder.append(needed ? ">" : "]").append(" ");
        }

        return ("/" + aliasUsed.toLowerCase() + " " + stringBuilder.toString().trim().toLowerCase());
    }

    public BaseComponent[] getUsage() {
        return this.usage;
    }

    public boolean execute(CommandSender sender, String[] args) {
        boolean isPlayer = sender instanceof Player;
        if (requirePlayer && !isPlayer) {
            sender.sendMessage(ChatColor.RED + "This command only works for in-game players");
            return true;
        }

        Player player = isPlayer ? (Player) sender : null;
        if (isPlayer && !canAccess(player)) {
            player.sendMessage(ChatColor.RED + "You need to be " + getRequiredRank().name().toLowerCase() + " to access this command.");
            return true;
        }

        Object[] parameters = new Object[getParameters().length + 1];
        parameters[0] = sender;
        int finalParamIndex = 1;
        int requiredParsed = 0;
        for (int paramIndex = 0, argIndex = 0; paramIndex < getParameters().length; paramIndex++, argIndex++) {
            ParamData param = getParameters()[paramIndex];
            String value;
            boolean consumed = true;
            if (param.isRequired()) {
                requiredParsed++;
                value = argIndex < args.length ? args[argIndex] : null;
            } else if (args.length - argIndex <= this.requiredCount - requiredParsed) {
                value = param.getDefaultValue();
                consumed = false;
            } else {
                value = argIndex < args.length ? args[argIndex] : param.getDefaultValue();
            }

            if (argIndex >= args.length && ("\0".equals(param.getDefaultValue()))) {
                if (isPlayer) {
                    player.spigot().sendMessage(this.usage);
                } else {
                    sender.sendMessage(this.getUsageString());
                }
                return false;
            }

            Object result;
            if (!param.isVarArgs()) {
                if (param.isWildcard() && argIndex < args.length) {
                    assert value != null;
                    StringBuilder sb = new StringBuilder(value);
                    for (++argIndex; argIndex < args.length; argIndex++) {
                        sb.append(' ').append(args[argIndex]);
                    }
                    value = sb.toString();
                }
                result = CommandManager.getInstance().transformParameter(player, value, param);
            } else {
                String[] varargs = new String[args.length - argIndex];
                System.arraycopy(args, argIndex, varargs, 0, varargs.length);
                result = CommandManager.getInstance().transformParameter(player, varargs, param);
            }

            if (result == null) {
                return false;
            } else if (!consumed && argIndex >= 0) {
                argIndex--;
            }
            parameters[finalParamIndex++] = result;
        }
        return this.verify(sender, parameters, 1);
    }

    private boolean verify(CommandSender player, Object[] parameters, int cursor) {
        Runnable task = new Runnable() {
            private final CommandData that = CommandData.this;

            @Override
            public void run() {
                try {
                    that.method.invoke(that.instance, parameters);
                } catch (Exception e) {
                    player.sendMessage(ChatColor.RED + "It appears there were some issues processing your command...");
                    e.printStackTrace();
                }
            }
        };

        if (this.async) {
            Bukkit.getScheduler().runTaskAsynchronously(FeatherPlugin.get(), task);
        } else {
            Bukkit.getScheduler().runTask(FeatherPlugin.get(), task);
        }
        return true;
    }

    public String[] getNames() {
        return this.names;
    }

    public String[] getFlags() {
        return this.flags;
    }

    public Method getMethod() {
        return this.method;
    }

    public boolean isHideFromHelp() {
        return hideFromHelp;
    }

    public boolean testFilter(String filter) {
        return this.filters.contains(filter);
    }

}

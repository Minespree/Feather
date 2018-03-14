package net.minespree.feather.command.system;

import com.mojang.api.profiles.HttpProfileRepository;
import com.mojang.api.profiles.Profile;
import com.mojang.api.profiles.ProfileRepository;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.minespree.babel.Babel;
import net.minespree.feather.FeatherPlugin;
import net.minespree.feather.command.system.annotation.Command;
import net.minespree.feather.command.system.annotation.Param;
import net.minespree.feather.command.system.bukkit.PluginCommandProxy;
import net.minespree.feather.command.system.objects.CommandData;
import net.minespree.feather.command.system.objects.ErrorHandler;
import net.minespree.feather.command.system.objects.ParamData;
import net.minespree.feather.command.system.objects.ParameterTransformer;
import net.minespree.feather.player.NetworkPlayer;
import net.minespree.feather.player.PlayerManager;
import net.minespree.feather.player.rank.Rank;
import net.minespree.feather.util.ArrayUtil;
import net.minespree.feather.util.LoggingUtils;
import net.minespree.feather.util.Reference;
import net.minespree.feather.util.UUIDNameKeypair;
import net.minespree.wizard.util.Chat;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandMap;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;

import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;
import java.util.function.BiFunction;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.regex.Pattern;
import java.util.stream.Collectors;


public class CommandManager implements Listener {

    private static final int PAGE_SIZE = 12;
    private static final Pattern WHITESPACE = Pattern.compile(" ");
    private static CommandManager instance;
    private final Map<String, org.bukkit.command.Command> knownCommands;
    private Map<String, CommandData> commands = new TreeMap<>(String::compareTo);
    private Map<Class<?>, ParameterTransformer> parameterTransformers = new HashMap<>();
    private Set<Class<?>> registeredClasses = new HashSet<>();

    private ProfileRepository repository = new HttpProfileRepository("minecraft");

    {
        BiFunction<Object, Class<?>, Object> getPropertyOfType = (obj, type) -> {
            Reference<ReflectiveOperationException> caught = new Reference<>();
            return Arrays.stream(obj.getClass().getDeclaredFields())
                    .filter(field -> type.isAssignableFrom(field.getType()))
                    .findFirst()
                    .map(field -> {
                        try {
                            field.setAccessible(true);
                            return field.get(obj);
                        } catch (ReflectiveOperationException ex) {
                            caught.setValue(ex);
                            return null;
                        }
                    }).orElseThrow(() -> new RuntimeException("Couldn't find any fields of type " + type.getName() + " in object of type " + obj.getClass().getName(),
                            caught.getValue()));
        };
        Map<String, org.bukkit.command.Command> known;
        try {
            Object cm = getPropertyOfType.apply(Bukkit.getServer(), CommandMap.class);
            known = (Map<String, org.bukkit.command.Command>) getPropertyOfType.apply(cm, Map.class);
        } catch (RuntimeException ex) {
            ex.printStackTrace();
            known = null;
        }
        this.knownCommands = known;
    }

    private CommandManager() {
        registerTransformer(int.class, (sender, source) -> {
            try {
                return (Integer.valueOf(source));
            } catch (NumberFormatException exception) {
                sender.sendMessage(Chat.RED + "The value " + source + Chat.RED + " is not a valid integer.");
                return (null);
            }
        });

        registerTransformer(long.class, (sender, source) -> {
            try {
                return Long.valueOf(source);
            } catch (Exception e) {
                sender.sendMessage(ChatColor.RED + "The value " + source + " is not a valid float.");
                return null;
            }
        });

        registerTransformer(Player.class, new ParameterTransformer() {
            @Override
            public Object transform(CommandSender sender, String source) {
                if (source.equalsIgnoreCase("self") || source.equals("")) {
                    return (sender);
                }

                Player player = Bukkit.getPlayer(source);

                if (player == null) {
                    sender.sendMessage(Chat.RED + "No player matched query for " + source);
                    return (null);
                }

                return (player);
            }

            @Override
            public List<String> complete(CommandSender sender, String arg) {
                if (arg.length() > 2) {
                    return Bukkit.matchPlayer(arg).stream().map(Player::getName).collect(Collectors.toList());
                }
                return Collections.emptyList();
            }
        });

        registerTransformer(UUID.class, new ParameterTransformer() {
            @Override
            public Object transform(CommandSender sender, String source) {
                for (Player p : Bukkit.getOnlinePlayers()) {
                    if (p.getName().equalsIgnoreCase(source)) {
                        return p.getUniqueId();
                    }
                }

                Profile[] profiles = repository.findProfilesByNames(source);
                if (profiles.length == 1) {
                    return profiles[0].getUUID();
                } else {
                    sender.sendMessage(Chat.RED + "The specified player " + source + " does not exist.");
                    return null;
                }
            }

            @Override
            public List<String> complete(CommandSender sender, String arg) {
                if (arg.length() > 2) {
                    return Bukkit.matchPlayer(arg).stream().map(Player::getName).collect(Collectors.toList());
                }
                return Collections.emptyList();
            }
        });

        registerTransformer(Rank.class, (sender, source) -> {
            try {
                return (Rank.valueOf(source.toUpperCase()));
            } catch (IllegalArgumentException e) {
                sender.sendMessage(Chat.RED + "No ranked matched the query for " + source);
                return (null);
            }
        });

        registerTransformer(boolean.class, (sender, source) -> {
            if (source.equalsIgnoreCase("true") || source.equalsIgnoreCase("yes") || source.equalsIgnoreCase("y")) {
                return true;
            } else if (source.equalsIgnoreCase("false") || source.equalsIgnoreCase("no") || source.equalsIgnoreCase("n")) {
                return false;
            } else {
                sender.sendMessage(Chat.RED + "Provided value " + source + Chat.RED + " is not valid for type Boolean.");
                return null;
            }
        });

        registerTransformer(UUIDNameKeypair.class, new ParameterTransformer() {
            @Override
            public Object transform(CommandSender sender, String source) {
                for (Player p : Bukkit.getOnlinePlayers()) {
                    if (p.getName().equalsIgnoreCase(source)) {
                        return new UUIDNameKeypair(p.getUniqueId(), p.getName());
                    }
                }

                Profile[] profiles = repository.findProfilesByNames(source);
                if (profiles.length == 1) {
                    return new UUIDNameKeypair(profiles[0].getUUID(), profiles[0].getName());
                } else {
                    sender.sendMessage(Chat.RED + "The specified player " + source + " does not exist.");
                    return null;
                }
            }

            @Override
            public List<String> complete(CommandSender sender, String arg) {
                if (arg.length() > 2) {
                    return Bukkit.matchPlayer(arg).stream().map(Player::getName).collect(Collectors.toList());
                }
                return Collections.emptyList();
            }
        });

        registerClass(CommandManager.class);

        Bukkit.getPluginManager().registerEvents(new Listener() {
            @EventHandler
            public void on(PlayerCommandPreprocessEvent event) {
                String message = event.getMessage();
                message = message.substring(message.indexOf('/') + 1);
                String[] args = message.split(" ");

                String command = args[0].toLowerCase();
                if (command.startsWith("minecraft:") || command.startsWith("bukkit:") || command.equalsIgnoreCase("me") ||
                        command.equalsIgnoreCase("plugins") || command.equalsIgnoreCase("pl") ||
                        command.equalsIgnoreCase("ver") || command.equalsIgnoreCase("version") ||
                        command.equalsIgnoreCase("about")) {
                    Babel.translate("disabled_command").sendMessage(event.getPlayer());
                    event.setCancelled(true);
                }
            }

//            @EventHandler
//            public void on(TabCompleteEvent event) {
//                String command = event.getBuffer().toLowerCase();
//                if (command.startsWith("/about") || command.startsWith("/minecraft:") || command.startsWith("/bukkit:")) {
//                    event.setCancelled(true);
//                } else {
//                    // TODO: double-check if this is right
//                    List<String> cleaned = event.getCompletions().stream()
//                            .filter(c -> !checkBanned(c))
//                            .collect(Collectors.toList());
//                    event.setCompletions(cleaned);
//                }
//            }

            private boolean checkBanned(String command) {
                return command.startsWith("/about") || command.startsWith("/minecraft:") || command.startsWith("/bukkit:");
            }
        }, FeatherPlugin.get());

        if (this.knownCommands == null) {
            LoggingUtils.log(Level.WARNING, "Falling back to old command handling");
            Bukkit.getPluginManager().registerEvents(this, FeatherPlugin.get());
//            try {
//                PacketManager.register(new PacketListener() {
//                    @Override
//                    public void receive(PacketEvent event) {
//                        if (!event.getPacket().is(PacketType.In.TAB_COMPLETE)) {
//                            return;
//                        }
//                        String command = event.getPacket().read("a", String.class);
//                        if (command.charAt(0) != '/') {
//                            return;
//                        }
//                        command = command.substring(1);
//                        String[] args = command.split(" ");
//                        int index = 1;
//
//                        String cmd = args[0].toLowerCase();
//
//                        CommandData found = null;
//                        if (args.length > 1) {
//                            found = CommandManager.this.commands.get(cmd + " " + args[1].toLowerCase());
//                        }
//                        if (found == null) {
//                            found = CommandManager.this.commands.get(cmd);
//                        } else {
//                            index = 2;
//                        }
//
//                        if (found == null || args.length < index) {
//                            return;
//                        }
//
//                        int lastArg = Math.min(args.length - 1 - index, found.getParameters().length - index);
//                        String last = args[lastArg + index];
//                        ParamData data = found.getParameters()[lastArg];
//                        String[] complete = CommandManager.this.parameterTransformers.getOrDefault(data.getParameterClass(), ParameterTransformer.NONE)
//                                .complete(event.getPlayer(), last).toArray(new String[0]);
//                        if (complete.length > 0) {
//                            ((CraftPlayer) event.getPlayer()).getHandle().playerConnection.sendPacket(new PacketPlayOutTabComplete(complete));
//                            event.setCancelled(true);
//                        }
//                    }
//                });
//            } catch (Throwable t) {
//                LoggingUtils.error(t);
//            }
        }
    }

    // TODO Move command to different class
    @Command(names = {"list"}, requiredRank = Rank.MEMBER)
    public static void listPlayers(Player sender) {
        Set<NetworkPlayer> players = PlayerManager.getInstance().getPlayers().stream()
                .filter(NetworkPlayer::isLoaded)
                .sorted((player1, player2) -> {
                    int rank1 = player1.getRank().ordinal();
                    int rank2 = player2.getRank().ordinal();

                    // 0 is lowest rank, return reverse result
                    return Integer.compare(rank2, rank1);
                })
                .collect(Collectors.toCollection(LinkedHashSet::new));

        int playerCount = players.size();
        ComponentBuilder builder = new ComponentBuilder("");
        sender.sendMessage(Chat.GRAY + Chat.SEPARATOR);

        Iterator<NetworkPlayer> iterator = players.iterator();

        while (iterator.hasNext()) {
            NetworkPlayer player = iterator.next();
            boolean last = !iterator.hasNext();

            builder.append(player.getPlayer().getDisplayName());
            builder.event(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/pm " + player.getName()));

            if (!last) {
                builder.append(", ").color(ChatColor.GRAY);
            }
        }

        sender.spigot().sendMessage(builder.create());

        sender.sendMessage(Chat.GRAY + "There are " + Chat.GOLD + playerCount + Chat.GRAY + " players on your server");
        sender.sendMessage(Chat.GRAY + Chat.SEPARATOR);
    }

    @Command(names = {"help", "?"}, requiredRank = Rank.MEMBER)
    public static void listCommands(Player sender,
                                    @Param(defaultValue = "", description = "Filter the commands based on a plugin or category") String filter,
                                    @Param(name = "Page", defaultValue = "1") int page) {
        if (page <= 0) {
            sender.sendMessage(Chat.GRAY + Chat.RED + "There is no such page.");
            return;
        }

        try {
            page = Integer.parseInt(filter);
            filter = "";
        } catch (NumberFormatException ignored) {
        }

        Predicate<CommandData> commandFilter = command -> true;
        if (!filter.isEmpty()) {
            final String keyword = filter;
            commandFilter = command -> command.testFilter(keyword);
        }

        CommandManager commandManager = getInstance();
        page = Math.max(page - 1, 0);
        Rank rank = NetworkPlayer.of(sender).getRank();
        List<CommandData> allCommands = commandManager.commands.values().stream()
                .filter(command -> rank.has(command.getRequiredRank()))
                .filter(commandFilter)
                .filter(command -> !command.isHideFromHelp())
                .distinct()
                .collect(Collectors.toList());
        int start = cap(page * PAGE_SIZE, 0, allCommands.size()), end = cap(start + PAGE_SIZE, 0, allCommands.size());
        int totalSize = allCommands.size();
        allCommands = allCommands.subList(start, end);

        if (allCommands.isEmpty()) {
            sender.sendMessage(Chat.RED + "There is no such page.");
            return;
        }
        sender.sendMessage(" ");
        sender.sendMessage(Chat.GREEN + Chat.BOLD + "Command List:");
        for (CommandData commandData : allCommands) {
            sender.spigot().sendMessage(commandData.getUsage());
        }
        sender.sendMessage(Chat.YELLOW + " ");
        BaseComponent[] prefix = TextComponent.fromLegacyText(Chat.YELLOW + "");
        ComponentBuilder builder = new ComponentBuilder(Chat.SMALL_ARROWS_LEFT).color(ChatColor.GRAY);
        if (page > 0) {
            builder.color(ChatColor.GREEN)
                    .event(new HoverEvent(HoverEvent.Action.SHOW_TEXT, TextComponent.fromLegacyText("Previous page")))
                    .event(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/help " + (!filter.isEmpty() ? filter + " " : "") + page));
        }
        BaseComponent[] left = builder.create();
        BaseComponent[] pages = TextComponent.fromLegacyText(Chat.GRAY + " Page " + (page + 1) + "/" + ((totalSize / 12) + 1) + " ");
        builder = new ComponentBuilder(Chat.SMALL_ARROWS_RIGHT).color(ChatColor.GRAY);
        if (end < totalSize) {
            builder.color(ChatColor.GREEN)
                    .event(new HoverEvent(HoverEvent.Action.SHOW_TEXT, TextComponent.fromLegacyText("Next page")))
                    .event(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/help " + (!filter.isEmpty() ? filter + " " : "") + (page + 2)));
        }
        BaseComponent[] right = builder.create();
        sender.spigot().sendMessage(ArrayUtil.concat(BaseComponent[].class, prefix, left, pages, right));
        sender.sendMessage(" ");
    }

    private static int cap(int i, int min, int max) {
        return Math.max(Math.min(i, max), min);
    }

    public static CommandManager getInstance() {
        if (instance == null) {
            synchronized (CommandManager.class) {
                if (instance == null) {
                    instance = new CommandManager();
                }
            }
        }
        return instance;
    }

    public void registerTransformer(Class<?> transforms, ParameterTransformer transformer) {
        this.parameterTransformers.put(transforms, transformer);
    }

    public void registerClass(Class<?> registeredClass) {
        if (this.registeredClasses.contains(registeredClass)) {
            return;
        }
        this.registeredClasses.add(registeredClass);
        Supplier<?> instance = getInstanceSupplier(registeredClass);
        for (Method method : registeredClass.getMethods()) {
            if (method.isAnnotationPresent(Command.class)) {
                registerMethod(method, instance);
            }
        }
    }

    private <T> Supplier<T> getInstanceSupplier(Class<T> cls) {
        Reference<T> ref = new Reference<>();
        return () -> {
            if (ref.getValue() == null) {
                synchronized (ref) {
                    if (ref.getValue() == null) {
                        try {
                            Constructor<T> construct = cls.getConstructor();
                            construct.setAccessible(true);
                            ref.setValue(construct.newInstance());
                        } catch (ReflectiveOperationException ex) {
                            throw new RuntimeException(ex);
                        }
                    }
                }
            }
            return ref.getValue();
        };
    }

    public void registerMethod(Method method) {
        throw new UnsupportedOperationException("Fix your code, registerMethod shouldn't even be public");
    }

    public void registerMethod(Method method, Supplier<?> instanceSupplier) {
        Command command = method.getAnnotation(Command.class);
        if (command.names().length < 0) {
            LoggingUtils.log(Level.INFO, "Command " + command.names()[0] + " does not have any names.");
            return;
        }
        Parameter[] parameters = method.getParameters();
        ParamData[] paramData = new ParamData[parameters.length - 1];
        for (int i = 1; i < parameters.length; i++) {
            Parameter param = parameters[i];

            if (param.isAnnotationPresent(Param.class)) {
                paramData[i - 1] = new ParamData(param);
            } else {
                LoggingUtils.log(Level.INFO, "Command " + command.names()[0] + " does not have a Param annotation on parameter " + (i + 1) + ".");
                return;
            }

            if (paramData[i - 1].isWildcard() && i < parameters.length - 1) {
                LoggingUtils.log(Level.INFO, "Command " + command.names()[0] + " registered a wildcard n ");
            }
        }

        CommandData data = new CommandData(Modifier.isStatic(method.getModifiers()) ? null : instanceSupplier.get(), method, command, paramData);
        registerCommand(data);
    }

    private void registerCommand(CommandData data) {
        Arrays.stream(data.getNames()).map(String::toLowerCase).forEach(name -> this.commands.put(name, data));
        if (this.knownCommands != null) {
            Arrays.stream(data.getNames()).map(String::toLowerCase).forEach(name -> {
                String[] nameSplit = WHITESPACE.split(name);
                String[] args = new String[nameSplit.length - 1];
                if (args.length > 0) {
                    System.arraycopy(nameSplit, 1, args, 0, args.length);
                }
                ((PluginCommandProxy) this.knownCommands.compute(nameSplit[0], ($, command) -> command instanceof PluginCommandProxy ? command : new PluginCommandProxy(name))).attach(args, data);
            });
        }
    }

    @EventHandler
    private void onCommandPreProcess(PlayerCommandPreprocessEvent event) {
        String message = event.getMessage();
        message = message.substring(message.indexOf('/') + 1);
        String[] args = message.split(" ");

        int index = 1;

        String command = args[0].toLowerCase();
        if (command.contains("minecraft:") || command.equalsIgnoreCase("me") || command.equalsIgnoreCase("plugins") || command.equalsIgnoreCase("pl") || command.equalsIgnoreCase("ver") || command.equalsIgnoreCase("version")) {
            Babel.translate("disabled_command").sendMessage(event.getPlayer());
            event.setCancelled(true);
            return;
        }

        CommandData found = null;
        if (args.length > 1) {
            found = this.commands.get(command + " " + args[1].toLowerCase());
        }
        if (found == null) {
            found = this.commands.get(command);
        } else {
            index = 2;
        }

        if (found == null) {
            return;
        }

        // Pop off first arg(s) (which is/are the cmd name)
        String[] tmp = new String[args.length - index];
        System.arraycopy(args, index, tmp, 0, tmp.length);
        args = tmp;

        event.setCancelled(true);

        if (!found.canAccess(event.getPlayer())) {
            event.getPlayer().sendMessage(Chat.RED + "Not enough permissions, " + found.getRequiredRank().getColor() + found.getRequiredRank().getTag() + Chat.RED + " or higher needed.");
            return;
        }

        found.execute(event.getPlayer(), args);
    }

    public Object transformParameter(Player sender, String[] parameterArray, ParamData target) {
        Class<?> cls = target.getParameterClass();
        assert cls.isArray();
        if (cls.getComponentType() == String.class) {
            return parameterArray;
        }
        ParameterTransformer transformer = this.parameterTransformers.get(cls.getComponentType());
        Object[] param = (Object[]) Array.newInstance(cls.getComponentType(), parameterArray.length);
        ErrorHandler handler = transformer.getErrorHandler(transformer, target, null);
        boolean valid = true;
        try {
            for (int i = 0; i < parameterArray.length; i++) {
                param[i] = transformer.transform(sender, parameterArray[i]);
                if (!handler.verify(param[i])) {
                    valid = false;
                    break;
                }
            }
        } catch (Exception ex) {
            handler.setException(ex);
        }
        if (handler.hasException() || !valid) {
            return handler;
        }
        return param;
    }

    public Object transformParameter(CommandSender sender, String parameter, ParamData target) {
        Class<?> cls = target.getParameterClass();
        if (String.class.equals(cls)) {
            return parameter;
        }
        return this.parameterTransformers.get(cls).tryTransform(sender, parameter, target);
    }

    public List<String> tabComplete(CommandSender sender, String parameter, ParamData target) {
        ParameterTransformer transformer = this.parameterTransformers.get(target.getParameterClass());
        return transformer != null ? transformer.complete(sender, parameter) : Collections.emptyList();
    }

    public List<CommandData> getCommands() {
        return new ArrayList<>(this.commands.values());
    }

    public CommandData getCommand(String name) {
        return this.commands.get(name.toLowerCase());
    }
}

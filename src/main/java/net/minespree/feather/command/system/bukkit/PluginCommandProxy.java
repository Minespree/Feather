package net.minespree.feather.command.system.bukkit;

import net.md_5.bungee.api.ChatColor;
import net.minespree.feather.command.system.objects.CommandData;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

import java.util.Collections;
import java.util.List;
import java.util.function.BiFunction;
import java.util.regex.Pattern;

public class PluginCommandProxy extends Command {

    private static final Pattern WHITESPACE = Pattern.compile(" ");

    private CommandNode base;

    private ThreadLocal<CommandData> context = new ThreadLocal<>();

    public PluginCommandProxy(String name) {
        super(name);
        this.base = new CommandNode(name, 0);
    }

    private CommandNode getRootNode() {
        return this.base;
    }

    public void attach(String[] args, CommandData data) {
        CommandNode node = getNode(args, CommandNode::getOrCreate);
        node.setData(data);
    }

    public List<String> tabComplete(CommandSender sender, String[] args) {
        return this.tabComplete(sender, getName(), args);
    }

    private CommandNode getNode(String[] args, BiFunction<CommandNode, String, CommandNode> func) {
        CommandNode current = getRootNode();
        for (int i = 0; i < args.length; i++) {
            CommandNode next = func.apply(current, args[i]);
            if (next != null) {
                current = next;
            } else {
                break;
            }
        }
        return current;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String alias, String[] args) throws IllegalArgumentException {
        try {
            CommandNode node = getNode(args, CommandNode::getNodeIfPresent);
            if (node.getDepth() > 0) {
                String[] tmp = new String[args.length - node.getDepth()];
                System.arraycopy(args, node.getDepth(), tmp, 0, tmp.length);
                args = tmp;
            }
            return node.onTabComplete(sender, this, alias, args);
        } catch (Throwable t) {
            t.printStackTrace();
            return Collections.emptyList();
        }
    }

    @Override
    public boolean execute(CommandSender sender, String alias, String[] args) {
        try {
            CommandNode node = getNode(args, CommandNode::getNodeIfPresent);
            if (node.getDepth() > 0) {
                String[] tmp = new String[args.length - node.getDepth()];
                System.arraycopy(args, node.getDepth(), tmp, 0, tmp.length);
                args = tmp;
            }
            return node.onCommand(sender, this, alias, args);
        } catch (Throwable t) {
            t.printStackTrace();
            sender.sendMessage(ChatColor.RED + "Uh oh, something seems to have gone wrong while executing the command. (This error has automagically been reported)");
            return true;
        }
    }

    @Override
    public List<String> getAliases() {
        return Collections.emptyList(); // FIXME?
    }

    @Override
    public String getPermissionMessage() {
        return ChatColor.RED + "Your rank must be equal to or higher than " + this.context.get().getRequiredRank().getTag()
                + ChatColor.RESET + ChatColor.RED + " in order to be allowed to access this command.";
    }

    @Override
    public String getDescription() {
        return "Just another command, you b-baka!"; // FIXME
    }

    @Override
    public Command setAliases(List<String> aliases) {
        throw new UnsupportedOperationException("Nope");
    }

    @Override
    public String getUsage() {
        return this.getRootNode().getUsage();
    }

    @Override
    public Command setDescription(String description) {
        throw new UnsupportedOperationException("Nope");
    }

    @Override
    public Command setPermissionMessage(String permissionMessage) {
        throw new UnsupportedOperationException("Nope");
    }

    @Override
    public Command setUsage(String usage) {
        throw new UnsupportedOperationException("Nope");
    }
}

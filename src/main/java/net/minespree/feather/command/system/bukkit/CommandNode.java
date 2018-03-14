package net.minespree.feather.command.system.bukkit;

import net.minespree.feather.command.system.CommandManager;
import net.minespree.feather.command.system.objects.CommandData;
import net.minespree.feather.command.system.objects.ParamData;
import net.minespree.feather.player.NetworkPlayer;
import net.minespree.feather.util.LoggingUtils;
import org.apache.commons.lang.StringUtils;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

public class CommandNode implements CommandExecutor, TabCompleter {

    private final String name;

    private final int depth;

    private CommandData data;
    private Map<String, CommandNode> nodes = new HashMap<>();

    CommandNode(String name, int depth) {
        this.name = name;
        this.depth = depth;
    }

    public CommandNode getOrCreate(String nodeName) {
        return this.nodes.computeIfAbsent(nodeName, $ -> {
            return new CommandNode(nodeName, getDepth() + 1);
        });
    }

    public CommandNode getNodeIfPresent(String nodeName) {
        return this.nodes.get(nodeName);
    }

    public void setData(CommandData data) {
        if (this.data != null) {
            LoggingUtils.log(Level.WARNING, "Duplicate command detected");
        }
        this.data = data;
    }

    public int getDepth() {
        return this.depth;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String alias, String[] args) {
        return this.data != null && this.data.execute(sender, args);
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (this.data == null) {
            return Collections.emptyList();
        }

        List<String> tab = new ArrayList<>();
        if (args.length == 1) {
            this.nodes.forEach((key, value) -> {
                if (key.startsWith(args[0]) && value.hasPermission(sender)) {
                    tab.add(key);
                }
            });
        }

        ParamData[] params = this.data.getParameters();
        int index = Math.min(args.length, params.length) - 1;
        // If we already have more params, but it's not a wildcard.
        if (params.length - 1 == index && args.length > params.length && !params[params.length - 1].isWildcard()) {
            return tab;
        }
        ParamData param = params[index];
        tab.addAll(CommandManager.getInstance().tabComplete(sender, StringUtils.join(args, "", index, args.length), param));
        return tab;
    }

    public boolean hasPermission(CommandSender sender) {
        if (!(sender instanceof Player)) {
            return sender.isOp();
        } else {
            NetworkPlayer gp = NetworkPlayer.of((Player) sender);
            return gp != null && this.data != null && gp.getRank().has(this.data.getRequiredRank());
        }
    }

    public String getUsage() {
        return this.data != null ? this.data.getUsageString() : "Incorrect command usage";
    }
}

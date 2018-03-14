package net.minespree.feather.command;

import com.github.kevinsawicki.timeago.TimeAgo;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.minespree.feather.command.system.annotation.Command;
import net.minespree.feather.command.system.annotation.Param;
import net.minespree.feather.player.NetworkPlayer;
import net.minespree.feather.player.PlayerManager;
import net.minespree.feather.player.rank.Rank;
import net.minespree.feather.util.UUIDNameKeypair;
import net.minespree.wizard.util.Chat;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.text.DecimalFormat;
import java.util.Date;

public class StaffCommands {

    @Command(names = {"playerinfo", "userinfo", "viewinfo"}, requiredRank = Rank.HELPER, async = true)
    public static void playerInfo(Player sender, @Param(name = "Target") UUIDNameKeypair target) {
        Player p = Bukkit.getPlayer(target.getUuid());

        if (p != null) {
            NetworkPlayer np = PlayerManager.getInstance().getPlayer(p);
            printData(np, sender);
            return;
        }

        NetworkPlayer data = PlayerManager.getInstance().getPlayer(target.getUuid(), true);
        if (data != null) {
            printData(data, sender);
            PlayerManager.getInstance().removePlayer(target.getUuid());
        } else {
            sender.sendMessage(ChatColor.RED + "That player is not in the database.");
        }
    }

    private static void printData(NetworkPlayer np, Player sender) {
        TimeAgo ago = new TimeAgo();

        ComponentBuilder builder = new ComponentBuilder(np.getPrefix() != null && !np.getPrefix().isEmpty() ? np.getPrefix() + np.getLastKnownUsername() : np.getRank().getColoredTag() + np.getLastKnownUsername());
        double xp = np.getNeededZeroToOne() * 100;
        DecimalFormat format = new DecimalFormat("#.##");
        builder.append("\n  " + Chat.YELLOW + "Level: " + Chat.GRAY + np.getLevel() + " " + Chat.DARK_GRAY + "(" + np.getExperience() + "xp) " + Chat.AQUA + "(" + format.format(xp) + "%)");
        builder.append("\n  " + Chat.YELLOW + "UUID: " + Chat.GRAY + np.getUuid().toString());
        builder.event(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, np.getUuid().toString()));
        builder.append("\n  " + Chat.YELLOW + "First Join: " + Chat.GRAY + ago.timeAgo(np.getFirstJoin()) + Chat.DARK_GRAY + " (" + new Date(np.getFirstJoin()).toGMTString() + ")");
        builder.append("\n  " + Chat.YELLOW + "Last Join: " + Chat.GRAY + ago.timeAgo(np.getLastJoin()) + Chat.DARK_GRAY);
        builder.append("\n" + Chat.DARK_GRAY + "(Click here to view punishments)");
        builder.event(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/viewhistory " + np.getLastKnownUsername()));

        sender.spigot().sendMessage(builder.create());
    }
}

package net.minespree.feather.command;

import net.minespree.babel.Babel;
import net.minespree.feather.command.system.annotation.Command;
import net.minespree.feather.command.system.annotation.Param;
import net.minespree.feather.db.redis.JedisPublisher;
import net.minespree.feather.player.NetworkPlayer;
import net.minespree.feather.player.PlayerManager;
import net.minespree.feather.player.rank.Rank;
import net.minespree.feather.util.LoggingUtils;
import net.minespree.feather.util.TimeUtils;
import net.minespree.feather.util.UUIDNameKeypair;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.logging.Level;

public class BuycraftCommands {

    @Command(names = {"buycraftrank"}, requiredRank = Rank.ADMIN, hideFromHelp = true, async = true)
    public static void buycraftRank(CommandSender sender, @Param(name = "Target") UUIDNameKeypair target, @Param(name = "rank") Rank rank) {
        Player p = Bukkit.getPlayer(target.getUuid());
        if (p != null) {
            NetworkPlayer np = PlayerManager.getInstance().getPlayer(p);

            if (np.isMonthlyRank()) {
                np.setMonthlyRank(false);
            }

            np.getPurchasedPackages().add("RankPackage-" + rank.name());
            np.setRank(rank);
            sender.sendMessage(ChatColor.GREEN + "Successfully set " + np.getLastKnownUsername() + "'s rank to " + rank.getColoredTag());
            Babel.translate("rank-updated").sendMessage(p, rank.getColoredTag());
            return;
        }

        NetworkPlayer data = PlayerManager.getInstance().getPlayer(target.getUuid(), true);
        if (data != null) {
            if (data.getRank().has(rank)) {
                sender.sendMessage(ChatColor.RED + target.getName() + " already has " + rank.name() + " or higher...");

                LoggingUtils.log(Level.INFO, "Not processing request, {0} already has {1} ({2}).", target.getName(), rank.name(), data.getRank().name());
                PlayerManager.getInstance().removePlayer(target.getUuid());
                return;
            }

            if (data.isMonthlyRank()) {
                data.setMonthlyRank(false);
            }

            data.getPurchasedPackages().add("RankPackage-" + rank.name());
            data.setRank(rank);
            sender.sendMessage(ChatColor.GREEN + "Successfully set " + target.getName() + "'s rank to " + rank.getColoredTag());

            JedisPublisher.create("rank-updates").set("uuid", target.getUuid().toString()).set("rank", rank.name()).publish();

            PlayerManager.getInstance().removePlayer(target.getUuid());
        } else {
            sender.sendMessage(ChatColor.RED + "That player is not in the database.");
        }
    }

    @Command(names = {"monthlyrank"}, requiredRank = Rank.ADMIN, hideFromHelp = true, async = true)
    public static void monthlyRank(CommandSender sender, @Param(name = "Target") UUIDNameKeypair target, @Param(name = "rank") Rank rank) {
        Player p = Bukkit.getPlayer(target.getUuid());
        if (p != null) {
            NetworkPlayer np = PlayerManager.getInstance().getPlayer(p);

            np.getPurchasedPackages().add("MonthlyRankPackage-" + rank.name() + "-" + TimeUtils.getDay());
            np.setRank(rank);
            np.setMonthlyRank(true);

            sender.sendMessage(ChatColor.GREEN + "Successfully set " + np.getLastKnownUsername() + "'s rank to " + rank.getColoredTag());
            Babel.translate("rank-updated").sendMessage(p, rank.getColoredTag());
            return;
        }

        NetworkPlayer data = PlayerManager.getInstance().getPlayer(target.getUuid(), true);
        if (data != null) {
            if (rank != Rank.MEMBER && data.getRank().has(rank)) {
                sender.sendMessage(ChatColor.RED + target.getName() + " already has " + rank.name() + " or higher...");

                LoggingUtils.log(Level.INFO, "Not processing request, {0} already has {1} ({2}).", target.getName(), rank.name(), data.getRank().name());
                PlayerManager.getInstance().removePlayer(target.getUuid());
                return;
            }

            data.getPurchasedPackages().add("MonthlyRankPackage-" + rank.name() + "-" + TimeUtils.getDay());
            data.setRank(rank);

            data.setMonthlyRank(true);

            sender.sendMessage(ChatColor.GREEN + "Successfully set " + target.getName() + "'s rank to " + rank.getColoredTag());

            JedisPublisher.create("rank-updates").set("uuid", target.getUuid().toString()).set("rank", rank.name()).publish();

            PlayerManager.getInstance().removePlayer(target.getUuid());
        } else {
            sender.sendMessage(ChatColor.RED + "That player is not in the database.");
        }
    }

    @Command(names = {"expiremonthly"}, requiredRank = Rank.ADMIN, hideFromHelp = true, async = true)
    public static void expireMonthly(CommandSender sender, @Param(name = "Target") UUIDNameKeypair target) {
        Player p = Bukkit.getPlayer(target.getUuid());
        if (p != null) {
            NetworkPlayer np = PlayerManager.getInstance().getPlayer(p);

            if (!np.isMonthlyRank()) {
                sender.sendMessage(ChatColor.RED + "Skipping request, player no longer has a monthly subscription.");
                return;
            }

            np.setRank(Rank.MEMBER);
            np.setMonthlyRank(false);

            sender.sendMessage(ChatColor.GREEN + "Successfully expired " + np.getLastKnownUsername() + "'s rank to member");
            Babel.translate("rank-updated").sendMessage(p, "Member");
            return;
        }

        NetworkPlayer data = PlayerManager.getInstance().getPlayer(target.getUuid(), true);
        if (data != null) {
            if (!data.isMonthlyRank()) {
                sender.sendMessage(ChatColor.RED + "Skipping request, player no longer has a monthly subscription.");
                return;
            }

            data.setRank(Rank.MEMBER);
            data.setMonthlyRank(false);

            sender.sendMessage(ChatColor.GREEN + "Successfully expired " + target.getName() + "'s rank to member");

            JedisPublisher.create("rank-updates").set("uuid", target.getUuid().toString()).set("rank", "Member").publish();

            PlayerManager.getInstance().removePlayer(target.getUuid());
        }
    }

}

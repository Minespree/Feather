package net.minespree.feather.command;

import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.minespree.babel.Babel;
import net.minespree.feather.FeatherPlugin;
import net.minespree.feather.command.system.annotation.Command;
import net.minespree.feather.command.system.annotation.Param;
import net.minespree.feather.db.redis.JedisPublisher;
import net.minespree.feather.player.NetworkPlayer;
import net.minespree.feather.player.PlayerManager;
import net.minespree.feather.player.PlayerTracker;
import net.minespree.feather.player.nick.NickManager;
import net.minespree.feather.player.rank.Rank;
import net.minespree.feather.queue.QueueJoiner;
import net.minespree.feather.util.UUIDNameKeypair;
import net.minespree.wizard.util.Chat;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

public class EssentialCommands {
    private static final String SET_BY = "setBy";

    @Command(names = {"setrank"}, requiredRank = Rank.ADMIN, async = true)
    public static void setRank(CommandSender sender, @Param(name = "Target") UUIDNameKeypair target, @Param(name = "rank") Rank rank) {
        Player player = Bukkit.getPlayer(target.getUuid());

        if (player != null) {
            NetworkPlayer np = PlayerManager.getInstance().getPlayer(player);
            setRankUpdate(sender, np, rank, null);

            Babel.translate("rank-updated").sendMessage(player, rank.getColoredTag());
        } else {
            NetworkPlayer data = PlayerManager.getInstance().getPlayer(target.getUuid(), true);

            if (data == null) {
                sender.sendMessage(ChatColor.RED + "That player is not in the database.");
                return;
            }

            setRankUpdate(sender, data, rank, target.getName());

            JedisPublisher.create("rank-updates").set("uuid", target.getUuid().toString()).set("rank", rank.name()).publish();

            PlayerManager.getInstance().removePlayer(target.getUuid());
        }
    }

    private static void setRankUpdate(CommandSender sender, NetworkPlayer player, Rank rank, String targetName) {
        player.setRank(rank);
        player.addUpdate(SET_BY, sender.getName());

        if (targetName == null) {
            targetName = rank.getSecondaryColor() + player.getLastKnownUsername();
        }

        sender.sendMessage(ChatColor.GREEN + "Successfully set " + targetName + Chat.GRAY + "'s rank to " + rank.getColoredTag().substring(0, rank.getColoredTag().length() - 1));

        if (sender instanceof Player) {
            JedisPublisher.create("staff-chat")
                    .set("message", ChatColor.RED + sender.getName() + ChatColor.GRAY + " set " + ChatColor.GREEN + targetName + ChatColor.GRAY + "'s rank to " + rank.getColoredTag().substring(0, rank.getColoredTag().length() - 1)).publish();
        }
    }

    @Command(names = "discord", requiredRank = Rank.MEMBER)
    public static void discord(Player player) {
        String message = Babel.translate("discord_message").toString(player);
        ComponentBuilder builder = new ComponentBuilder(message).event(new ClickEvent(ClickEvent.Action.OPEN_URL, "https://discord.gg/Ss3urNA"));
        player.spigot().sendMessage(builder.create());
    }

    @Command(names = "twitter", requiredRank = Rank.MEMBER)
    public static void twitter(Player player) {
        String message = Babel.translate("twitter_message").toString(player);
        ComponentBuilder builder = new ComponentBuilder(message).event(new ClickEvent(ClickEvent.Action.OPEN_URL, "https://twitter.com/minespree"));
        player.spigot().sendMessage(builder.create());
    }

    @Command(names = "rules", requiredRank = Rank.MEMBER)
    public static void rules(Player player) {
        Babel.translateMulti("rules_message").sendMessage(player);
    }

    @Command(names = "store", requiredRank = Rank.MEMBER)
    public static void store(Player player) {
        Babel.translateMulti("store_message").sendMessage(player);
    }

    @Command(names = "banana", requiredRank = Rank.MEMBER, hideFromHelp = true)
    public static void banana(Player player) {
        float r = ThreadLocalRandom.current().nextFloat();
        if (r <= 0.001f) {
            Babel.translate("supersuit_chance").sendMessage(player);
        } else {
            Babel.translate("did_you_mean_pineapple").sendMessage(player);
        }
    }

    @Command(names = "pineapple", requiredRank = Rank.MEMBER, hideFromHelp = true)
    public static void pineapple(Player player) {
        float r = ThreadLocalRandom.current().nextFloat();
        if (r <= 0.001f) {
            Babel.translate("honey_where_is_my_supersuit").sendMessage(player);
        } else {
            Babel.translate("praise_pineapple").sendMessage(player);
        }

        // NetworkPlayer.of(player).setAchievement(FeatherAchievements.PINEAPPLE, true);
    }

    @Command(names = "coins", requiredRank = Rank.MEMBER)
    public static void coins(Player player) {
        Babel.translate("coins_total").sendMessage(player, NetworkPlayer.of(player).getCoins());
    }

    @Command(names = "gems", requiredRank = Rank.MEMBER)
    public static void gems(Player player) {
        Babel.translate("gems_total").sendMessage(player, NetworkPlayer.of(player).getGems());
    }

    @Command(names = {"whereis", "findplayer", "donthidefromme"}, requiredRank = Rank.HELPER, async = true)
    public static void find(Player player, @Param(name = "Target") UUIDNameKeypair target) {
        PlayerTracker tracker = FeatherPlugin.get().getPlayerTracker();
        if (!tracker.isOnline(target.getUuid())) {
            Babel.translate("target_not_online").sendMessage(player);
            return;
        }

        String server = tracker.getServerAt(target.getUuid());
        String message = Babel.translate("target_on_server").toString(player, target.getName(), server);

        ComponentBuilder builder = new ComponentBuilder(message)
                .event(new HoverEvent(HoverEvent.Action.SHOW_TEXT, TextComponent.fromLegacyText(ChatColor.GRAY + "Click to be sent to " + server)))
                .event(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/goto server " + server));

        player.spigot().sendMessage(builder.create());
    }

    @Command(names = {"goto player", "stp"}, requiredRank = Rank.HELPER, async = true)
    public static void gotoPlayer(Player player, @Param(name = "Target") UUIDNameKeypair target) {
        PlayerTracker tracker = FeatherPlugin.get().getPlayerTracker();
        if (!tracker.isOnline(target.getUuid())) {
            Babel.translate("target_not_online").sendMessage(player);
            return;
        }

        String server = tracker.getServerAt(target.getUuid());
        tracker.connect(player, server);
    }

    @Command(names = {"goto server"}, requiredRank = Rank.HELPER)
    public static void gotoServer(Player player, @Param(name = "Target Server") String server) {
        PlayerTracker tracker = FeatherPlugin.get().getPlayerTracker();
        tracker.connect(player, server);
    }

    @Command(names = {"nick", "disguise"}, requiredRank = Rank.VIP)
    public static void nick(Player player) {
        NickManager.NickSetup.start(NetworkPlayer.of(player));
    }

    @Command(names = {"unnick", "undisguise", "nick reset"}, requiredRank = Rank.VIP)
    public static void unnick(Player player) {
        FeatherPlugin.get().getNickManager().resetNick(NetworkPlayer.of(player));
    }

    @Command(names = {"queue"}, requiredRank = Rank.MEMBER)
    public static void queueStatus(Player player) {
        String queuedGame = FeatherPlugin.get().getQueueJoiner().getQueuedGame(player.getUniqueId());

        if (queuedGame != null) {
            Babel.translate("queue_status").sendMessage(player, queuedGame);
        } else {
            Babel.translate("queue_notin").sendMessage(player);
        }
    }

    @Command(names = {"queue leave"}, requiredRank = Rank.MEMBER)
    public static void leaveQueue(Player player) {
        QueueJoiner queueJoiner = FeatherPlugin.get().getQueueJoiner();
        UUID uuid = player.getUniqueId();
        String queuedGame = queueJoiner.getQueuedGame(uuid);

        if (queuedGame == null) {
            Babel.translate("queue_notin").sendMessage(player);
            return;
        }

        queueJoiner.leaveQueue(uuid);
    }

}

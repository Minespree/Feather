package net.minespree.feather.command;

import com.google.common.util.concurrent.FutureCallback;
import net.minespree.babel.Babel;
import net.minespree.feather.FeatherPlugin;
import net.minespree.feather.command.system.annotation.Command;
import net.minespree.feather.command.system.annotation.Param;
import net.minespree.feather.data.chat.ChatManager;
import net.minespree.feather.db.redis.JedisPublisher;
import net.minespree.feather.player.Friends;
import net.minespree.feather.player.NetworkPlayer;
import net.minespree.feather.player.PlayerManager;
import net.minespree.feather.player.PlayerTracker;
import net.minespree.feather.player.rank.Rank;
import net.minespree.feather.util.UUIDNameKeypair;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import javax.annotation.Nullable;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

public class MessageCommands {
    @Command(names = {"toggle messaging"}, requiredRank = Rank.HELPER)
    public static void toggle(Player player) {
        NetworkPlayer np = NetworkPlayer.of(player);

        boolean previous = np.isStaffChat();
        previous = !previous;

        np.setStaffChat(previous);

        Babel.translate("public_messaging_toggle").sendMessage(player, previous ? "Enabled" : "Disabled");
    }

    @Command(names = {"msg", "tell", "pm", "whisper", "message"}, requiredRank = Rank.MEMBER, async = true)
    public static void message(Player player, @Param(name = "Target") UUIDNameKeypair target, @Param(name = "Message", wildcard = true) String message) {
        if (Objects.equals(player.getUniqueId(), target.getUuid())) {
            Babel.translate("message_self_disallowed").sendMessage(player);
            return;
        }

        PlayerTracker tracker = FeatherPlugin.get().getPlayerTracker();
        if (!tracker.isOnline(target.getUuid())) {
            Babel.translate("target_not_online").sendMessage(player);
            return;
        }

        NetworkPlayer sender = NetworkPlayer.of(player);
        NetworkPlayer targetPlayer;
        Player pTarget;
        boolean remove = false;
        if ((pTarget = Bukkit.getPlayer(target.getUuid())) != null) {
            targetPlayer = NetworkPlayer.of(pTarget);
        } else {
            remove = true;
            targetPlayer = PlayerManager.getInstance().getPlayer(target.getUuid(), true);
        }

        if (targetPlayer == null) {
            Babel.translate("messaging_failed_to_fetch").sendMessage(player, target.getName());
            return;
        }

        message = ChatColor.stripColor(message);

        if (targetPlayer.getRank().has(Rank.HELPER)) {
            if (targetPlayer.isStaffChat()) {
                sendMessage(sender, targetPlayer, message);
                if (remove) PlayerManager.getInstance().removePlayer(target.getUuid());
            } else {
                Babel.translate("staff_messaging_disabled").sendMessage(player);
            }
        } else if (sender.getRank().has(Rank.HELPER)) {
            sendMessage(sender, targetPlayer, message);
            if (remove) PlayerManager.getInstance().removePlayer(target.getUuid());
        } else {
            boolean finalRemove = remove;

            String finalMessage = message;
            Friends.get(targetPlayer.getUuid(), Friends.Status.FRIENDS, new FutureCallback<Set<UUID>>() {
                @Override
                public void onSuccess(@Nullable Set<UUID> uuids) {
                    if (uuids == null) {
                        onFailure(new IllegalArgumentException("friends null"));
                        return;
                    }

                    if (uuids.contains(player.getUniqueId())) {
                        sendMessage(sender, targetPlayer, finalMessage);
                        if (finalRemove) PlayerManager.getInstance().removePlayer(target.getUuid());
                    } else {
                        Babel.translate("cant_message_not_friends").sendMessage(player);
                    }
                }

                @Override
                public void onFailure(Throwable throwable) {
                    Babel.translate("error_occurred_messaging").sendMessage(player, throwable.getMessage());
                }
            });
        }
    }

    @Command(names = {"reply", "r", "respond"}, requiredRank = Rank.MEMBER, async = true)
    public static void reply(Player player, @Param(name = "Message", wildcard = true) String message) {
        NetworkPlayer np = NetworkPlayer.of(player);
        if (np.getLastMessager() == null) {
            Babel.translate("no_reply_to").sendMessage(player);
            return;
        }

        PlayerTracker tracker = FeatherPlugin.get().getPlayerTracker();
        if (!tracker.isOnline(np.getLastMessager())) {
            Babel.translate("last_no_longer_online").sendMessage(player);
            return;
        }

        NetworkPlayer targetPlayer;
        Player pTarget;
        boolean remove = false;
        if ((pTarget = Bukkit.getPlayer(np.getLastMessager())) != null) {
            targetPlayer = NetworkPlayer.of(pTarget);
        } else {
            remove = true;
            targetPlayer = PlayerManager.getInstance().getPlayer(np.getLastMessager(), true);
        }

        if (targetPlayer == null) {
            Babel.translate("messaging_failed_to_fetch").sendMessage(player, "Player");
            return;
        }

        message = ChatColor.stripColor(message);

        sendMessage(np, targetPlayer, message);
        if (remove) PlayerManager.getInstance().removePlayer(np.getLastMessager());
    }

    @Command(names = {"ignore"}, requiredRank = Rank.MEMBER, async = true)
    public static void ignore(Player player, @Param(name = "Player") UUIDNameKeypair ignore) {
        if (ignore.getUuid().equals(player.getUniqueId())) {
            Babel.translate("cant_ignore_self").sendMessage(player);
            return;
        }

        Player ignorePlayerBukkit = Bukkit.getPlayer(ignore.getUuid());
        NetworkPlayer ignorePlayer;
        if (ignorePlayerBukkit != null) {
            ignorePlayer = NetworkPlayer.of(ignorePlayerBukkit);
        } else {
            ignorePlayer = PlayerManager.getInstance().getPlayer(ignore.getUuid(), true);
        }

        if (ignorePlayer.getRank().has(Rank.HELPER)) {
            Babel.translate("cant_ignore_staff").sendMessage(player);
            return;
        }

        NetworkPlayer self = NetworkPlayer.of(player);
        if (self.getIgnoredPlayers().add(ignore.getUuid())) {
            Babel.translate("player_ignored").sendMessage(player, ignore.getName());
        } else {
            Babel.translate("player_already_ignored").sendMessage(player, ignore.getName());
        }
    }

    @Command(names = {"unignore"}, requiredRank = Rank.MEMBER, async = true)
    public static void unignore(Player player, @Param(name = "Player") UUIDNameKeypair ignore) {
        if (ignore.getUuid().equals(player.getUniqueId())) {
            Babel.translate("cant_ignore_self").sendMessage(player);
            return;
        }

        Player ignorePlayerBukkit = Bukkit.getPlayer(ignore.getUuid());
        NetworkPlayer ignorePlayer;
        if (ignorePlayerBukkit != null) {
            ignorePlayer = NetworkPlayer.of(ignorePlayerBukkit);
        } else {
            ignorePlayer = PlayerManager.getInstance().getPlayer(ignore.getUuid(), true);
        }

        if (ignorePlayer.getRank().has(Rank.HELPER)) {
            Babel.translate("cant_ignore_staff").sendMessage(player);
            return;
        }

        NetworkPlayer self = NetworkPlayer.of(player);
        if (self.getIgnoredPlayers().remove(ignore.getUuid())) {
            Babel.translate("player_unignored").sendMessage(player, ignore.getName());
        } else {
            Babel.translate("player_already_unignored").sendMessage(player, ignore.getName());
        }
    }

    private static void sendMessage(NetworkPlayer sender, NetworkPlayer target, String message) {
        sender.setLastMessager(target.getUuid());

        ChatManager.ChatData data = new ChatManager.ChatData(sender.getPlayer(),
                message, System.currentTimeMillis(),
                Bukkit.getServerName(), message, ChatManager.ChatType.PRIVATE, target.getUuid());
        ChatManager manager = FeatherPlugin.get().getChatManager();
        if (manager != null) {
            manager.insertMessage(data);
        }

        Babel.translate("message_to").sendMessage(sender.getPlayer(), target.getRank().getColor() + target.getLastKnownUsername(), message);

        JedisPublisher.create("private_messages").set("sender", sender.getUuid())
                .set("sender_name", sender.getRank().getColor() + sender.getLastKnownUsername())
                .set("target", target.getUuid())
                .set("message", message)
                .publish();
    }

}

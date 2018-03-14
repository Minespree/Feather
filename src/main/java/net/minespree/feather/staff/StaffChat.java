package net.minespree.feather.staff;

import net.minespree.babel.Babel;
import net.minespree.feather.FeatherPlugin;
import net.minespree.feather.command.system.CommandManager;
import net.minespree.feather.command.system.annotation.Command;
import net.minespree.feather.command.system.annotation.Param;
import net.minespree.feather.db.redis.JedisPublisher;
import net.minespree.feather.db.redis.RedisManager;
import net.minespree.feather.events.PlayerJoinNetworkEvent;
import net.minespree.feather.events.PlayerLeaveNetworkEvent;
import net.minespree.feather.player.NetworkPlayer;
import net.minespree.feather.player.rank.Rank;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import java.util.UUID;

public class StaffChat implements Listener {
    @Command(names = {"staffchat", "sc"}, requiredRank = Rank.HELPER)
    public static void staffChat(Player player, @Param(name = "Message", wildcard = true) String message) {
        NetworkPlayer np = NetworkPlayer.of(player);

        if (!np.staffChatEnabled()) {
            Babel.translate("staff_chat_alert").sendMessage(player);
            return;
        }

        String text = np.getRank().getColor() + player.getName() + ChatColor.DARK_GRAY + ": " + ChatColor.RESET + message;
        JedisPublisher.create("staff-chat").set("message", text).publish();
    }

    @Command(names = {"togglestaffchat", "tsc"}, requiredRank = Rank.HELPER)
    public static void toggleStaffChat(Player player) {
        NetworkPlayer np = NetworkPlayer.of(player);

        boolean previous = np.staffChatEnabled();
        previous = !previous;

        np.setStaffChat(previous);

        Babel.translate("staff_chat_toggle").sendMessage(player, previous ? "Enabled" : "Disabled");
    }

    public void hook() {
        RedisManager.getInstance().registerListener("staff-chat", (channel, object) -> {
            String message = (String) object.get("message");

            for (Player player : Bukkit.getOnlinePlayers()) {
                NetworkPlayer np = NetworkPlayer.of(player);
                if (np.getRank().has(Rank.HELPER) && np.staffChatEnabled()) {
                    player.sendMessage(ChatColor.AQUA + "[STAFF] " + ChatColor.RESET + message);
                }
            }
        });

        CommandManager.getInstance().registerClass(StaffChat.class);

        FeatherPlugin.get().getServer().getPluginManager().registerEvents(this, FeatherPlugin.get());
    }

    @EventHandler
    public void on(PlayerJoinNetworkEvent event) {
        UUID uuid = event.getUuid();
        Player p;
        if ((p = Bukkit.getPlayer(uuid)) != null) {
            NetworkPlayer np = NetworkPlayer.of(p);
            if (!np.getRank().has(Rank.VIP)) return;
            publishToStaff(np.getRank().getColor() + p.getName() + ChatColor.GRAY + " has connected.");
        }
    }

    @EventHandler
    public void on(PlayerLeaveNetworkEvent event) {
        UUID uuid = event.getUuid();
        Player p;
        if ((p = Bukkit.getPlayer(uuid)) != null) {
            NetworkPlayer np = NetworkPlayer.of(p);
            if (!np.getRank().has(Rank.VIP)) return;
            publishToStaff(np.getRank().getColor() + p.getName() + ChatColor.GRAY + " has disconnected.");
        }
    }

    public void publishToStaff(String message) {
        JedisPublisher.create("staff-chat").set("message", message).publish();
    }

}

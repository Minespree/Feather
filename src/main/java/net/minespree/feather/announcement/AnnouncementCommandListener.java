package net.minespree.feather.announcement;

import net.minespree.feather.db.redis.RedisListener;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.json.simple.JSONObject;

public class AnnouncementCommandListener implements RedisListener {
    @Override
    public void receive(String channel, JSONObject object) {
        if (channel.equals("announce")) {
            String msg = ChatColor.translateAlternateColorCodes('&', (String) object.get("message"));
            for (Player player : Bukkit.getOnlinePlayers()) {
                player.sendMessage(" ");
                player.sendMessage(msg);
                player.sendMessage(" ");
            }
        }
    }
}

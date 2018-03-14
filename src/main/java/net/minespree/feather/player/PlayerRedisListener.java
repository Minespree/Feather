package net.minespree.feather.player;

import net.minespree.feather.db.redis.RedisListener;
import net.minespree.feather.events.NetworkEvent;
import net.minespree.feather.events.PlayerJoinNetworkEvent;
import net.minespree.feather.events.PlayerLeaveNetworkEvent;
import org.bukkit.Bukkit;
import org.json.simple.JSONObject;

import java.util.UUID;

public class PlayerRedisListener implements RedisListener {
    @Override
    public void receive(String channel, JSONObject object) {
        UUID player = UUID.fromString((String) object.get("player"));
        NetworkEvent event;

        if ("player-join".equals(channel)) {
            event = new PlayerJoinNetworkEvent(player);
        } else if ("player-quit".equals(channel)) {
            event = new PlayerLeaveNetworkEvent(player);
        } else {
            throw new IllegalStateException("invalid channel listening");
        }

        Bukkit.getPluginManager().callEvent(event);
    }
}

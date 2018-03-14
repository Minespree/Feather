package net.minespree.feather.data.chat;

import net.minespree.babel.Babel;
import net.minespree.feather.db.redis.RedisListener;
import net.minespree.feather.player.NetworkPlayer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.json.simple.JSONObject;

import java.util.UUID;

public class PrivateMessagingRedisListener implements RedisListener {
    @Override
    public void receive(String channel, JSONObject object) {
        UUID sender = UUID.fromString((String) object.get("sender"));
        String senderName = (String) object.get("sender_name");
        UUID target = UUID.fromString((String) object.get("target"));
        String message = (String) object.get("message");

        Player p;
        if ((p = Bukkit.getPlayer(target)) != null) {
            NetworkPlayer np = NetworkPlayer.of(p);
            np.setLastMessager(sender);

            Babel.translate("message_from").sendMessage(p, senderName, message);
        }
    }
}

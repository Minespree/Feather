package net.minespree.feather.data.whitelist;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import lombok.Getter;
import net.minespree.feather.FeatherPlugin;
import net.minespree.feather.db.redis.RedisManager;
import net.minespree.feather.player.NetworkPlayer;
import net.minespree.feather.player.rank.Rank;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import redis.clients.jedis.Jedis;

import java.util.concurrent.TimeUnit;

public class Whitelist {
    private static final long INTERVAL = TimeUnit.SECONDS.toMillis(15) * 20L;
    private static Whitelist instance;

    @Getter
    private WhitelistMode mode;

    public Whitelist(FeatherPlugin plugin) {
        initTask(plugin);
        instance = this;
    }

    public static Whitelist getInstance() {
        return instance;
    }

    public static void checkLogin(AsyncPlayerPreLoginEvent event, NetworkPlayer player) {
        if (instance == null || instance.getMode() == null) {
            event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_OTHER, "Error while getting whitelist value");
            return;
        }

        WhitelistMode mode = instance.getMode();

        if ((mode == WhitelistMode.STAFF && !player.getRank().has(Rank.HELPER)) || (mode == WhitelistMode.VIP && !player.getRank().has(Rank.VIP))) {
            event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_OTHER, ChatColor.RED + "The network is currently undergoing maintenance.");
        } else {
            event.allow();
        }
    }

    public void initTask(FeatherPlugin plugin) {
        Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, () -> {
            try (Jedis jedis = RedisManager.getInstance().getPool().getResource()) {
                String whitelist = jedis.get("whitelist");

                if (whitelist == null) {
                    return;
                }

                try {
                    JsonElement element = new JsonParser().parse(whitelist);
                    JsonObject ob = element.getAsJsonObject();
                    mode = WhitelistMode.valueOf(ob.get("mode").getAsString());
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }, 0L, INTERVAL);
    }

}

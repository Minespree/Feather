package net.minespree.feather.util;

import com.google.common.base.Preconditions;
import net.minespree.feather.FeatherPlugin;
import net.minespree.feather.db.redis.JedisPublisher;
import net.minespree.feather.db.redis.RedisManager;
import org.bukkit.Bukkit;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;

public class RestartManager {

    private static final String CHANNEL = "restart-manager";
    private AtomicBoolean shouldRestart = new AtomicBoolean(false);

    public RestartManager() {
        RedisManager.getInstance().registerListener(CHANNEL, (channel, object) -> {
            String target = (String) object.get("targetServer");
            String serverName = Bukkit.getServerName();
            Preconditions.checkNotNull(target, "target");
            Preconditions.checkNotNull(serverName, "serverName");

            if (serverName.contains(target) || serverName.equalsIgnoreCase(target)) {
                shouldRestart.set(true);

                FeatherPlugin.get().getLogger().log(Level.INFO, "Scheduled restart when server is empty.");
            }
        });

        Bukkit.getScheduler().runTaskTimer(FeatherPlugin.get(), () -> {
            if (shouldRestart.get() && Bukkit.getServer().getOnlinePlayers().isEmpty()) {
                Bukkit.shutdown();
            }
        }, 0L, 20L);
    }

    public void scheduleRestartForType(String type) {
        JedisPublisher.create(CHANNEL).set("targetServer", type)
                .publish();
    }
}

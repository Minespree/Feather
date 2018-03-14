package net.minespree.feather.queue;

import com.google.common.base.Joiner;
import com.google.common.collect.Maps;
import net.minespree.feather.db.redis.RedisManager;
import net.minespree.feather.player.NetworkPlayer;
import net.minespree.feather.util.Scheduler;
import redis.clients.jedis.Jedis;

import java.util.Map;
import java.util.UUID;

public class QueueJoiner {
    private static final String REDIS_CHANNEL = "msGameQueue";
    private static final Joiner REDIS_JOINER = Joiner.on("###");

    private RedisManager manager;
    private Map<UUID, String> queued;

    public QueueJoiner(RedisManager manager) {
        this.manager = manager;
        this.queued = Maps.newHashMap();
    }

    public boolean joinQueue(UUID uuid, String game) {
        if (queued.containsKey(uuid)) {
            return false;
        }

        String message = REDIS_JOINER.join("join", uuid.toString(), game, "std");
        queued.put(uuid, game);

        publishMessage(message);
        return true;
    }

    public boolean joinQueue(NetworkPlayer player, String game) {
        return joinQueue(player.getUuid(), game);
    }

    public boolean leaveQueue(UUID uuid) {
        if (!queued.containsKey(uuid)) {
            return false;
        }

        String message = REDIS_JOINER.join("leave", uuid.toString());
        removeFromQueue(uuid);

        publishMessage(message);
        return true;
    }

    public boolean leaveQueue(NetworkPlayer player) {
        return leaveQueue(player.getUuid());
    }

    public String getQueuedGame(UUID uuid) {
        return queued.get(uuid);
    }

    public String getQueuedGame(NetworkPlayer player) {
        return getQueuedGame(player.getUuid());
    }

    public boolean isQueued(UUID uuid) {
        return getQueuedGame(uuid) != null;
    }

    public boolean isQueued(NetworkPlayer player) {
        return isQueued(player.getUuid());
    }

    private void publishMessage(String message) {
        Scheduler.runAsync(() -> {
            try (Jedis jedis = manager.getPool().getResource()) {
                jedis.publish(REDIS_CHANNEL, message);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    public void removeFromQueue(UUID uuid) {
        queued.remove(uuid);
    }
}

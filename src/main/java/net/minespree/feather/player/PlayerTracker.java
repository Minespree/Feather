package net.minespree.feather.player;

import com.google.common.base.Preconditions;
import com.google.common.collect.Maps;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import net.minespree.feather.FeatherPlugin;
import net.minespree.feather.db.redis.RedisManager;
import org.bukkit.entity.Player;
import redis.clients.jedis.Jedis;

import java.util.Collection;
import java.util.Map;
import java.util.UUID;

public class PlayerTracker {
    public String getShardKey(UUID uuid) {
        return "player-server:" + getShardAt(uuid);
    }

    public String getShardAt(UUID uuid) {
        Preconditions.checkNotNull(uuid);

        return uuid.toString().substring(0, 1);
    }

    public boolean isOnline(UUID uuid) {
        try (Jedis jedis = getResource()) {
            String shardKey = getShardKey(uuid);

            return jedis.hexists(shardKey, uuid.toString());
        }
    }

    public Map<UUID, Boolean> areOnline(Collection<UUID> uuids) {
        Map<UUID, Boolean> results = Maps.newHashMapWithExpectedSize(uuids.size());

        try (Jedis jedis = getResource()) {
            for (UUID uuid : uuids) {
                String shardKey = getShardKey(uuid);

                boolean online = jedis.hexists(shardKey, uuid.toString());
                results.put(uuid, online);
            }
        }

        return results;
    }

    public String getServerAt(UUID uuid) {
        try (Jedis jedis = getResource()) {
            String shardKey = getShardKey(uuid);

            return jedis.hget(shardKey, uuid.toString());

        }
    }

    public Map<UUID, String> getServerAt(Collection<UUID> uuids) {
        Map<UUID, String> servers = Maps.newHashMapWithExpectedSize(uuids.size());

        try (Jedis jedis = getResource()) {
            for (UUID uuid : uuids) {
                String shardKey = getShardKey(uuid);

                String server = jedis.hget(shardKey, uuid.toString());
                servers.put(uuid, server);
            }
        }

        return servers;
    }

    public Map<UUID, String> getServerAt(Collection<UUID> uuids, String defaultServer) {
        Map<UUID, String> servers = getServerAt(uuids);

        servers.forEach((key, value) -> {
            if (value == null) {
                servers.put(key, defaultServer);
            }
        });

        return servers;
    }

    public void connect(Player player, String server) {
        ByteArrayDataOutput o = ByteStreams.newDataOutput();
        o.writeUTF("Connect");
        o.writeUTF(server);
        player.sendPluginMessage(FeatherPlugin.get(), "BungeeCord", o.toByteArray());
    }

    private Jedis getResource() {
        return RedisManager.getInstance().getPool().getResource();
    }
}

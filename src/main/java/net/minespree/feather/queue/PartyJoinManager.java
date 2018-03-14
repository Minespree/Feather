package net.minespree.feather.queue;

import com.google.common.base.Preconditions;
import com.google.common.collect.Sets;
import net.minespree.feather.FeatherPlugin;
import net.minespree.feather.db.redis.RedisManager;
import net.minespree.feather.events.PartyJoinEvent;
import net.minespree.feather.player.NetworkPlayer;
import net.minespree.feather.util.Scheduler;
import org.bukkit.scheduler.BukkitTask;
import redis.clients.jedis.Jedis;

import java.util.Set;
import java.util.UUID;

/**
 * Class that manages party joins and awaits until all players
 * have joined the server and their data has been loaded to
 * call a {@link net.minespree.feather.events.PartyJoinEvent}
 */
public class PartyJoinManager implements Runnable {
    // Check every 5 seconds
    private static final long EXPIRED_CHECK = 5 * 20L;
    private final FeatherPlugin plugin;
    private final Set<PartyJoin> joins = Sets.newHashSet();
    private final PartyJoinSubscriber subscriber;
    private final BukkitTask expiredTask;
    private Jedis subscriberJedis;

    public PartyJoinManager(FeatherPlugin plugin) {
        this.plugin = plugin;
        this.subscriber = new PartyJoinSubscriber(this, plugin.getServer().getServerName());

        // Register subscriber
        Scheduler.runAsync(() -> {
            try (Jedis jedis = subscriberJedis = RedisManager.getInstance().getPool().getResource()) {
                jedis.subscribe(subscriber, PartyJoinSubscriber.PARTY_CHANNEL);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        expiredTask = plugin.getServer().getScheduler().runTaskTimer(plugin, this, EXPIRED_CHECK, EXPIRED_CHECK);
    }

    public static void register() {
        FeatherPlugin plugin = FeatherPlugin.get();
        Preconditions.checkArgument(plugin.getPartyJoinManager() == null, "Join manager is already registered");

        plugin.setPartyJoinManager(new PartyJoinManager(plugin));
    }

    public static void unregister() {
        FeatherPlugin plugin = FeatherPlugin.get();
        PartyJoinManager manager = plugin.getPartyJoinManager();

        Preconditions.checkNotNull(manager);

        manager.unregisterSelf();
        plugin.setPartyJoinManager(null);
    }

    public void addPartyJoin(PartyJoin join) {
        joins.add(join);
    }

    public void notifyLoad(NetworkPlayer player) {
        UUID uuid = player.getUuid();

        joins.forEach(join -> {
            if (join.awaits(uuid)) {
                boolean allJoined = join.notifyJoin(uuid);

                if (allJoined) {
                    onJoin(join);
                }
            }
        });
    }

    private void onJoin(PartyJoin join) {
        removeJoin(join);

        PartyJoinEvent event = join.toEvent();
        plugin.getServer().getPluginManager().callEvent(event);
    }

    @Override
    public void run() {
        joins.forEach(join -> {
            if (join.hasExpired()) {
                removeJoin(join);
            }
        });
    }

    private void removeJoin(PartyJoin join) {
        joins.remove(join);
    }

    private void unregisterSelf() {
        if (subscriberJedis != null) {
            try {
                subscriberJedis.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        expiredTask.cancel();
    }
}

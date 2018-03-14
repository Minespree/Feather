package net.minespree.feather.player;

import com.google.common.collect.Maps;
import lombok.Getter;
import net.minespree.babel.Babel;
import net.minespree.feather.FeatherPlugin;
import net.minespree.feather.db.mongo.MongoManager;
import net.minespree.feather.db.redis.RedisManager;
import net.minespree.feather.events.NetworkPlayerLoadEvent;
import net.minespree.feather.player.loaders.MongoPlayerLoader;
import net.minespree.feather.player.loaders.PlayerLoader;
import net.minespree.feather.player.rank.Rank;
import net.minespree.feather.player.save.MongoPlayerSaver;
import net.minespree.feather.player.save.PlayerSaver;
import net.minespree.feather.util.LoggingUtils;
import org.bson.BSON;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.Collection;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Supplier;
import java.util.logging.Level;

public class PlayerManager {
    private static final PlayerManager instance = new PlayerManager();
    private static final long SAVE_SPAN = 20L;
    private static final long LOAD_DURATION = TimeUnit.SECONDS.toMillis(10L);

    static {
        BSON.addEncodingHook(UUID.class, String::valueOf);
    }

    private final Map<UUID, NetworkPlayer> players = Maps.newConcurrentMap();

    private PlayerFactory factory = NetworkPlayer::new;

    private PlayerLoader loader;
    @Getter
    private PlayerSaver saver;
    private BukkitTask task;

    public static PlayerManager getInstance() {
        return instance;
    }

    public void init() {
        loader = new MongoPlayerLoader(MongoManager.getInstance(), FeatherPlugin.get());
        saver = new MongoPlayerSaver(MongoManager.getInstance(), FeatherPlugin.get());
    }

    public void startTask() {
        if (task != null) {
            task.cancel();
        }

        RedisManager.getInstance().registerListener("rank-updates", (channel, object) -> {
            UUID target = UUID.fromString((String) object.get("uuid"));
            Rank rank = Rank.valueOf((String) object.get("rank"));

            Player p;
            if ((p = Bukkit.getPlayer(target)) != null) {
                NetworkPlayer np = NetworkPlayer.of(p);
                np.setRank(rank);

                Babel.translate("rank-updated").sendMessage(p, rank.getColoredTag());
            }
        });

        this.task = Bukkit.getScheduler().runTaskTimerAsynchronously(FeatherPlugin.get(), () -> {
            players.values().forEach(this::attemptSave);
        }, SAVE_SPAN, SAVE_SPAN);
    }

    public NetworkPlayer getPlayer(Player player) {
        return getPlayer(player, false);
    }

    public NetworkPlayer getPlayer(Player player, boolean load) {
        return awaitLoad(player.getUniqueId(), load, getSupplier(player), true);
    }

    /**
     * Won't bootstrap the player's data or call a {@link NetworkPlayerLoadEvent}
     * Don't forget to call {@link #removePlayer(UUID)} when you're done reading/writing.
     */
    public NetworkPlayer loadOfflinePlayer(UUID uuid) {
        return awaitLoad(uuid, true, getSupplier(uuid), false);
    }

    public NetworkPlayer awaitLoad(UUID uuid, boolean load, Supplier<? extends NetworkPlayer> supplier, boolean bootstrap) {
        NetworkPlayer current = players.get(uuid);

        if (current != null || !load) {
            return current;
        }

        return loadPlayer(uuid, supplier, bootstrap);
    }

    private NetworkPlayer loadPlayer(UUID uuid, Supplier<? extends NetworkPlayer> supplier, boolean bootstrap) {
        CompletableFuture<? extends NetworkPlayer> future = loader.loadPlayer(uuid, supplier, bootstrap);

        try {
            NetworkPlayer player = future.get(LOAD_DURATION, TimeUnit.MILLISECONDS);

            players.put(uuid, player);

            // Don't call event if player didn't get bootstrapped
            if (bootstrap) {
                Bukkit.getPluginManager().callEvent(new NetworkPlayerLoadEvent(player));
            }

            return player;
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
            LoggingUtils.log(Level.SEVERE, "Error loading {0}''s data", uuid);
        } catch (TimeoutException ignored) {
            LoggingUtils.log(Level.SEVERE, "Player data loading timeout for {0} (took >{1} ms)", uuid, LOAD_DURATION);
        }

        return null;
    }

    public NetworkPlayer getPlayer(UUID uuid) {
        return getPlayer(Bukkit.getPlayer(uuid));
    }

    public NetworkPlayer getPlayer(UUID uuid, boolean load) {
        return awaitLoad(uuid, load, getSupplier(uuid), true);
    }

    public void removePlayer(Player player) {
        removePlayer(player.getUniqueId());
    }

    public void removePlayer(UUID uuid) {
        NetworkPlayer player = players.get(uuid);

        if (player != null) {
            // Save changes before unloading
            attemptSave(player, true);

            players.remove(uuid, player);
        }
    }

    public void attemptSave(NetworkPlayer player, boolean force) {
        if (!force && !player.shouldSave(SAVE_SPAN)) {
            return;
        }

        saver.save(player).whenCompleteAsync((saved, throwable) -> {
            if (throwable != null) {
                throwable.printStackTrace();
            }
        });
    }

    public void attemptSave(NetworkPlayer player) {
        attemptSave(player, false);
    }

    public Collection<NetworkPlayer> getPlayers() {
        return players.values();
    }

    private Supplier<? extends NetworkPlayer> getSupplier(Player player) {
        return getSupplier(player.getUniqueId());
    }

    private Supplier<? extends NetworkPlayer> getSupplier(UUID uuid) {
        return () -> factory.createPlayer(uuid);
    }

    public void setFactory(PlayerFactory factory) {
        this.factory = factory;
    }

    public interface PlayerFactory {
        NetworkPlayer createPlayer(UUID uuid);
    }
}

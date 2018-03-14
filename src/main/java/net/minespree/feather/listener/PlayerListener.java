package net.minespree.feather.listener;

import com.comphenix.protocol.ProtocolLibrary;
import com.google.common.net.InetAddresses;
import com.google.common.util.concurrent.FutureCallback;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import net.minespree.analytics.AnalyticsClient;
import net.minespree.analytics.AnalyticsEvent;
import net.minespree.babel.Babel;
import net.minespree.babel.BabelMessage;
import net.minespree.feather.FeatherPlugin;
import net.minespree.feather.data.gamedata.GameRegistry;
import net.minespree.feather.data.update.UpdateBook;
import net.minespree.feather.data.whitelist.Whitelist;
import net.minespree.feather.db.mongo.MongoManager;
import net.minespree.feather.events.PlayerJoinNetworkEvent;
import net.minespree.feather.events.PlayerLeaveNetworkEvent;
import net.minespree.feather.player.Friends;
import net.minespree.feather.player.NetworkPlayer;
import net.minespree.feather.player.PlayerManager;
import net.minespree.feather.player.rank.Rank;
import net.minespree.feather.player.stats.persitent.PersistentStatistics;
import net.minespree.feather.queue.PartyJoinManager;
import net.minespree.feather.util.ItemUtil;
import net.minespree.feather.util.Scheduler;
import net.minespree.feather.util.TimeUtils;
import net.minespree.feather.util.UUIDNameKeypair;
import org.bson.Document;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import javax.annotation.Nullable;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;

public class PlayerListener implements Listener {

    private BabelMessage FRIEND_JOINED = Babel.translate("notif-friend-joined");
    private BabelMessage FRIEND_LEFT = Babel.translate("notif-friend-left");

    private NavigableMap<Integer, String> versionMap = new TreeMap<Integer, String>() {{
        put(47, "18x");
        put(107, "19");
        put(108, "191");
        put(108, "192");
        put(109, "193-194");
        put(210, "110x");
        put(315, "1111");
        put(316, "1112-1113");
        put(335, "112");
        put(338, "1121");
        put(340, "1122");
    }};

    @EventHandler
    public void on(AsyncPlayerPreLoginEvent event) {
        NetworkPlayer np = PlayerManager.getInstance().getPlayer(event.getUniqueId(), true);

        if (np == null || !np.isLoaded()) {
            event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_OTHER, "Failed to instantiate " + np.getClass().getSimpleName() + ", please contact staff.");
            return;
        }

        MongoCollection<Document> punishments = MongoManager.getInstance().getCollection("punishments");
        // TODO Move to new player loading system
        Scheduler.runAsync(() -> punishments.find(Filters.and(Filters.eq("target", event.getUniqueId().toString()), Filters.eq("type", "MUTE"),
                Filters.eq("undone", false), Filters.lt("until", System.currentTimeMillis())))
                .sort(new Document("until", -1)).first(), new FutureCallback<Document>() {
            @Override
            public void onSuccess(@Nullable Document document) {
                if (document != null) {
                    np.setMuted(document.getLong("until"));
                }
            }

            @Override
            public void onFailure(Throwable throwable) {
                // nop
            }
        });

        Whitelist.checkLogin(event, np);
    }

    // Lowest priority is always first
    @EventHandler(priority = EventPriority.LOWEST)
    public void on(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        event.setJoinMessage(null);

        NetworkPlayer np = NetworkPlayer.of(player);

        if (np == null) {
            event.getPlayer().kickPlayer("Failed to instantiate NetworkPlayer, please contact staff. [JE]");
            return;
        }

        np.setPlayer(player);
        np.getKnownIps().add(InetAddresses.toAddrString(player.getAddress().getAddress()));
        np.updateTag();

        if (np.getRank().has(Rank.ADMIN)) {
            event.getPlayer().setOp(true);
        }

        Friends.get(np.getUuid(), Friends.Status.FRIENDS, new FutureCallback<Set<UUID>>() {
            @Override
            public void onSuccess(@Nullable Set<UUID> uuids) {
                if (uuids == null) {
                    return;
                }
                np.setFriends(new LinkedHashSet<>(uuids));
            }

            @Override
            public void onFailure(Throwable throwable) {
                throwable.printStackTrace();
            }
        }, Scheduler.getPublicExecutor());

        action(event.getPlayer(), AnalyticAction.JOIN);
    }

    // Allow all plugins to clean up before.
    @EventHandler(priority = EventPriority.HIGHEST)
    public void on(PlayerQuitEvent event) {
        Player player = event.getPlayer();

        PlayerManager.getInstance().removePlayer(player);
        FeatherPlugin.get().getQueueJoiner().removeFromQueue(player.getUniqueId());

        event.setQuitMessage(null);

        action(player, AnalyticAction.QUIT);
    }

    @EventHandler
    public void on(PlayerJoinNetworkEvent event) {
        UUID uuid = event.getUuid();
        Player p;
        if ((p = Bukkit.getPlayer(uuid)) != null) {
            NetworkPlayer np = NetworkPlayer.of(p);

            int mcVer = ProtocolLibrary.getProtocolManager().getProtocolVersion(p);
            String closestVer = getClosestVersion(mcVer);

            PersistentStatistics stats = np.getPersistentStats();
            stats.getIntegerStatistics(GameRegistry.Type.GLOBAL)
                    .increment("daily_joins_" + TimeUtils.getDay(), 1)
                    .increment("weekly_joins_" + TimeUtils.getWeek(), 1)
                    .increment("monthly_joins_" + TimeUtils.getMonth(), 1)
                    .increment("totalJoinsMcVersion" + closestVer, 1);

            stats.getLongStatistics(GameRegistry.Type.GLOBAL)
                    .set("latestJoinMcVersion" + closestVer, System.currentTimeMillis());

            stats.getStringStatistics(GameRegistry.Type.GLOBAL)
                    .set("lastPlayedVersion", closestVer);

            stats.persist();

            Bukkit.getScheduler().runTaskLater(FeatherPlugin.get(), () -> {
                int version = np.getUpdateVersion();
                UpdateBook book = FeatherPlugin.get().getUpdateBook();

                if (book != null && book.getStack() != null && (version == -1 || version < book.getVersion())) {
                    ItemUtil.openBook(book.getStack(), p);

                    np.setUpdateVersion(book.getVersion());
                }
            }, 40L);

            PartyJoinManager manager = FeatherPlugin.get().getPartyJoinManager();

            if (manager != null) {
                manager.notifyLoad(np);
            }
        }

        for (Player other : Bukkit.getOnlinePlayers()) {
            if (other.getUniqueId().equals(uuid)) continue;

            NetworkPlayer op = NetworkPlayer.of(other);
            if (op.getFriends().contains(uuid)) {
                UUIDNameKeypair.generate(uuid, keypair -> {
                    if (keypair != null) {
                        FRIEND_JOINED.sendMessage(other, keypair.getName());
                    }
                });
            }
        }

        join(event.getUuid());
    }

    @EventHandler
    public void on(PlayerLeaveNetworkEvent event) {
        UUID uuid = event.getUuid();

        for (Player other : Bukkit.getOnlinePlayers()) {
            if (other.getUniqueId().equals(uuid)) continue;

            NetworkPlayer op = NetworkPlayer.of(other);
            if (op.getFriends().contains(uuid)) {
                UUIDNameKeypair.generate(uuid, keypair -> {
                    if (keypair != null) {
                        FRIEND_LEFT.sendMessage(other, keypair.getName());
                    }
                });
            }
        }
    }

    private void join(UUID player) {
        AnalyticsClient client = AnalyticsClient.getClient();
        AnalyticsEvent event = client.event(AnalyticsClient.USER_CATEGORY);
        event.newEvent(player.toString());
        event.terminateEvent();

        sendAsync(event);
    }

    private void sendAsync(AnalyticsEvent event) {
        Bukkit.getScheduler().runTaskAsynchronously(FeatherPlugin.get(), () -> {
            try {
                event.send();
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    private void action(Player player, AnalyticAction action) {
        AnalyticsClient client = AnalyticsClient.getClient();
        AnalyticsEvent event = client.event(AnalyticsClient.DESIGN_CATEGORY);

        event.newEvent(player.getUniqueId().toString())
                .withEventId("Server:" + action.name())
                .withArea(Bukkit.getServerName())
                .terminateEvent();

        sendAsync(event);
    }

    private String getClosestVersion(int version) {
        Map.Entry<Integer, String> low = versionMap.floorEntry(version);
        Map.Entry<Integer, String> high = versionMap.ceilingEntry(version);
        String res = null;
        if (low != null && high != null) {
            res = Math.abs(version - low.getKey()) < Math.abs(version - high.getKey())
                    ? low.getValue()
                    : high.getValue();
        } else if (low != null || high != null) {
            res = low != null ? low.getValue() : high.getValue();
        }
        return res;
    }


    enum AnalyticAction {
        JOIN,
        QUIT
    }

}

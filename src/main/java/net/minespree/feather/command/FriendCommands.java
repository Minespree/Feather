package net.minespree.feather.command;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.util.concurrent.FutureCallback;
import lombok.Getter;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.minespree.babel.Babel;
import net.minespree.babel.BabelMessage;
import net.minespree.feather.FeatherPlugin;
import net.minespree.feather.command.system.annotation.Command;
import net.minespree.feather.command.system.annotation.Param;
import net.minespree.feather.data.games.GameType;
import net.minespree.feather.data.games.ServerType;
import net.minespree.feather.db.redis.JedisPublisher;
import net.minespree.feather.db.redis.RedisListener;
import net.minespree.feather.player.Friends;
import net.minespree.feather.player.NetworkPlayer;
import net.minespree.feather.player.PlayerManager;
import net.minespree.feather.player.PlayerTracker;
import net.minespree.feather.player.rank.Rank;
import net.minespree.feather.util.MapSorter;
import net.minespree.feather.util.Scheduler;
import net.minespree.feather.util.UUIDNameKeypair;
import net.minespree.wizard.util.Chat;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.json.simple.JSONObject;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class FriendCommands {

    private static final int PAGE_SIZE = 7;
    private static final String OFFLINE_SERVER = "OFFLINE_SERVER";
    private static BabelMessage ERROR_NO_PAGE = Babel.translate("no-such-page");
    private static BabelMessage ONLINE_FRIENDS = Babel.translate("online-friends");
    private static BabelMessage FRIEND_MSG = Babel.translate("friend-playing");
    private static BabelMessage PAGES = Babel.translate("friends-pages");
    private static BabelMessage NO_FRIENDS = Babel.translate("error-no-friends");
    private static BabelMessage NO_FRIENDS_ONLINE = Babel.translate("error-no-friends-online");
    private static BabelMessage CANT_FRIEND_SELF = Babel.translate("cant-friend-self");
    private static BabelMessage FRIEND_REQUEST_SENT = Babel.translate("friend-request-sent");
    private static BabelMessage ALREADY_FRIENDS = Babel.translate("already-friends");
    private static BabelMessage FRIEND_ACCEPT_PENDING = Babel.translate("friend-accept-pending");
    private static BabelMessage ACCEPTED_REQUEST = Babel.translate("accepted-friend");
    private static BabelMessage NO_REQUEST = Babel.translate("no-friend-request");
    private static BabelMessage DENIED_REQUEST = Babel.translate("denied-friend");
    private static BabelMessage REMOVED_FRIEND = Babel.translate("removed-friend");
    private static BabelMessage NOT_FRIENDS = Babel.translate("not-friends");
    private static BabelMessage HAS_REQUESTED_FRIENDSHIP = Babel.translate("has-requested-friendship");
    private static BabelMessage OTHER_DENIED_REQUEST = Babel.translate("other-denied-request");
    private static BabelMessage NO_LONGER_FRIENDS = Babel.translate("no-longer-friends");

    @Command(names = {"friend list", "friends list", "friends", "f list", "f"}, requiredRank = Rank.MEMBER, async = true)
    public static void listFriends(Player player, @Param(name = "Page", defaultValue = "1") int page) {
        NetworkPlayer np = NetworkPlayer.of(player);
        if (page <= 0) {
            ERROR_NO_PAGE.sendMessage(player);
            return;
        }

        if (np.getFriends().isEmpty()) {
            NO_FRIENDS.sendMessage(player);
            return;
        }

        PlayerTracker tracker = FeatherPlugin.get().getPlayerTracker();
        page = Math.max(page - 1, 0);
        Set<UUID> friendUuids = np.getFriends();

        Map<UUID, String> unsortedServers = tracker.getServerAt(friendUuids, OFFLINE_SERVER);
        // Online players go first
        Map<UUID, String> servers = MapSorter.sortByValue(unsortedServers, (server1, server2) -> {
            // Player 1 is offline, sort down
            if (server1.equals(OFFLINE_SERVER)) {
                return -1;
            }

            // Player 2 is offline, sort down
            if (server2.equals(OFFLINE_SERVER)) {
                return 1;
            }

            return server1.compareTo(server2);
        });

        int totalSize = servers.size();
        int start = cap(page * PAGE_SIZE, 0, totalSize);
        int end = cap(start + PAGE_SIZE, 0, totalSize);

        final List<FriendData> friends = Lists.newArrayListWithExpectedSize(end - start);

        // We only want elements from start to end
        Iterable<UUID> uuids = Iterables.skip(servers.keySet(), start);
        uuids = Iterables.limit(uuids, PAGE_SIZE);

        uuids.forEach(uuid -> {
            String server = servers.get(uuid);
            FriendData data;

            if (!server.equals(OFFLINE_SERVER)) {
                data = new FriendData(uuid, server);
            } else {
                data = new OfflineFriendData(uuid);
            }

            friends.add(data);
        });

        int totalPages = (totalSize / PAGE_SIZE) + 1;
        int onlineFriends = getOnlineFriendsCount(servers);

        ONLINE_FRIENDS.sendMessage(player, onlineFriends, totalSize);

        for (FriendData data : friends) {
            ChatColor statusColor = data.isOnline() ? ChatColor.GREEN : ChatColor.RED;
            ChatColor bodyColor = data.isOnline() ? ChatColor.YELLOW : ChatColor.GRAY;
            String rawName = data.getRawName();
            ComponentBuilder builder = new ComponentBuilder("✦").color(statusColor);

            if (data.isOnline()) {
                // Party join
                builder.event(new HoverEvent(HoverEvent.Action.SHOW_TEXT, TextComponent.fromLegacyText(Chat.PINK + "Click to invite to Party!")));
                builder.event(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/party invite " + rawName));
            }

            builder.append(" ").append(data.getName());

            if (data.isOnline()) {
                builder.event(new HoverEvent(HoverEvent.Action.SHOW_TEXT, TextComponent.fromLegacyText(Chat.AQUA + "Click to Message")));
                // UX: Extra space for instant writing
                builder.event(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/msg " + rawName + " "));
            }

            builder.append(" is ").color(bodyColor);

            String status = data.getStatus();
            builder.append(status).color(bodyColor);

            if (data.canJoinServer(np)) {
                builder.event(new HoverEvent(HoverEvent.Action.SHOW_TEXT, TextComponent.fromLegacyText(Chat.GREEN + "Click to join server")));
                builder.event(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/gotofriend " + rawName));
            }

            player.spigot().sendMessage(builder.create());
        }

        player.sendMessage("");

        // TODO Add clickable pages support

        PAGES.sendMessage(player, (page + 1), totalPages);
    }

    private static int getOnlineFriendsCount(Map<UUID, String> servers) {
        int count = 0;

        for (String server : servers.values()) {
            if (server != null) {
                count++;
            }
        }

        return count;
    }

    @Command(names = {"gotofriend"}, requiredRank = Rank.MEMBER, async = true, hideFromHelp = true)
    public static void gotoFriend(Player player, @Param(name = "Target") UUIDNameKeypair target) {
        NetworkPlayer np = NetworkPlayer.of(player);

        if (np == null) return;

        PlayerTracker tracker = FeatherPlugin.get().getPlayerTracker();
        if (!tracker.isOnline(target.getUuid())) {
            Babel.translate("target_not_online").sendMessage(player);
            return;
        }

        String server = tracker.getServerAt(target.getUuid());

        if (!ServerType.canJoin(np, server)) {
            Babel.translate("target_cannot_join").sendMessage(player);
            return;
        }

        tracker.connect(player, server);
        player.sendMessage(ChatColor.GREEN + "Sending you to " + target.getName() + "'s server.");
    }

    @Command(names = {"friends add", "friend add", "f add"}, requiredRank = Rank.MEMBER, async = true)
    public static void addFriend(Player player, @Param(name = "Target") UUIDNameKeypair target) {
        if (player.getUniqueId().equals(target.getUuid())) {
            CANT_FRIEND_SELF.sendMessage(player);
            return;
        }

        NetworkPlayer np = NetworkPlayer.of(player);

        if (np.hasFriendRequestFrom(target.getUuid())) {
            acceptFriend(player, target);
            return;
        }

        Friends.createRequest(player.getUniqueId(), target.getUuid(), new FutureCallback<Friends.Response>() {
            @Override
            public void onSuccess(Friends.Response response) {
                if (response == Friends.Response.SUCCESS) {
                    FRIEND_REQUEST_SENT.sendMessage(player, target.getName());

                    JedisPublisher.create("friend-requests").set("from", player.getUniqueId().toString()).set("target", target.getUuid().toString()).publish();
                } else if (response == Friends.Response.FAILED_ALREADY_FRIENDS) {
                    ALREADY_FRIENDS.sendMessage(player, target.getName());
                } else {
                    FRIEND_ACCEPT_PENDING.sendMessage(player, target.getName());
                }
            }

            @Override
            public void onFailure(Throwable throwable) {
                throwable.printStackTrace();
            }
        }, Scheduler.getPublicExecutor());
    }

    @Command(names = {"friends accept", "friend accept", "f accept"}, requiredRank = Rank.MEMBER, async = true)
    public static void acceptFriend(Player player, @Param(name = "Target") UUIDNameKeypair target) {
        Friends.createFriendship(player.getUniqueId(), target.getUuid(), new FutureCallback<Friends.Response>() {
            @Override
            public void onSuccess(Friends.Response response) {
                String name;
                Player t;
                if ((t = Bukkit.getPlayer(target.getUuid())) != null) {
                    NetworkPlayer np = NetworkPlayer.of(t);
                    name = np.getRank().getSecondaryColor() + t.getName();
                } else {
                    NetworkPlayer np = PlayerManager.getInstance().getPlayer(target.getUuid(), true);
                    name = np.getRank().getSecondaryColor() + np.getLastKnownUsername();

                    PlayerManager.getInstance().removePlayer(target.getUuid());
                }

                if (response == Friends.Response.SUCCESS) {
                    ACCEPTED_REQUEST.sendMessage(player, name);

                    JedisPublisher.create("friend-acceptances").set("who", player.getUniqueId().toString()).set("from", target.getUuid().toString()).publish();

                    NetworkPlayer.of(player).addFriend(target.getUuid()); // add locally.
                } else if (response == Friends.Response.FAILED_NO_REQUEST) {
                    NO_REQUEST.sendMessage(player, target.getName());
                }
            }

            @Override
            public void onFailure(Throwable throwable) {
                throwable.printStackTrace();
            }
        }, Scheduler.getPublicExecutor());
    }

    @Command(names = {"friends deny", "friend deny", "f deny"}, requiredRank = Rank.MEMBER, async = true)
    public static void denyFriend(Player player, @Param(name = "Target") UUIDNameKeypair target) {
        Friends.deleteRequest(player.getUniqueId(), target.getUuid(), new FutureCallback<Friends.Response>() {
            @Override
            public void onSuccess(Friends.Response response) {
                String name;
                Player t;
                if ((t = Bukkit.getPlayer(target.getUuid())) != null) {
                    NetworkPlayer np = NetworkPlayer.of(t);
                    name = np.getRank().getSecondaryColor() + t.getName();
                } else {
                    NetworkPlayer np = PlayerManager.getInstance().getPlayer(target.getUuid(), true);
                    name = np.getRank().getSecondaryColor() + np.getLastKnownUsername();

                    PlayerManager.getInstance().removePlayer(target.getUuid());
                }

                if (response == Friends.Response.SUCCESS) {
                    DENIED_REQUEST.sendMessage(player, name);

                    JedisPublisher.create("friend-denials").set("who", player.getUniqueId().toString()).set("from", target.getUuid().toString()).publish();
                } else if (response == Friends.Response.FAILED_NO_REQUEST) {
                    NO_REQUEST.sendMessage(player, target.getName());
                }
            }

            @Override
            public void onFailure(Throwable throwable) {
                throwable.printStackTrace();
            }
        }, Scheduler.getPublicExecutor());
    }

    @Command(names = {"friends remove", "friend remove", "f remove"}, requiredRank = Rank.MEMBER, async = true)
    public static void removeFriend(Player player, @Param(name = "Target") UUIDNameKeypair target) {
        Friends.betray(player.getUniqueId(), target.getUuid(), new FutureCallback<Friends.Response>() {
            @Override
            public void onSuccess(Friends.Response response) {
                String name;
                Player t;
                if ((t = Bukkit.getPlayer(target.getUuid())) != null) {
                    NetworkPlayer np = NetworkPlayer.of(t);
                    name = np.getRank().getSecondaryColor() + t.getName();
                } else {
                    NetworkPlayer np = PlayerManager.getInstance().getPlayer(target.getUuid(), true);
                    name = np.getRank().getSecondaryColor() + np.getLastKnownUsername();

                    PlayerManager.getInstance().removePlayer(target.getUuid());
                }

                if (response == Friends.Response.SUCCESS) {
                    REMOVED_FRIEND.sendMessage(player, name);

                    JedisPublisher.create("friend-backstabs").set("who", player.getUniqueId().toString()).set("target", target.getUuid().toString()).publish();

                    NetworkPlayer.of(player).removeFriend(target.getUuid()); // remove locally.
                } else {
                    NOT_FRIENDS.sendMessage(player, name);
                }
            }

            @Override
            public void onFailure(Throwable throwable) {
                throwable.printStackTrace();
            }
        }, Scheduler.getPublicExecutor());
    }

    private static int cap(int i, int min, int max) {
        return Math.max(Math.min(i, max), min);
    }

    private static String getNameFromUuid(UUID uuid) {
        Player t;

        if ((t = Bukkit.getPlayer(uuid)) != null) {
            NetworkPlayer np = NetworkPlayer.of(t);
            return np.getRank().getSecondaryColor() + t.getName();
        } else {
            PlayerManager manager = PlayerManager.getInstance();
            NetworkPlayer player = manager.loadOfflinePlayer(uuid);

            manager.removePlayer(uuid);

            return player.getRank().getSecondaryColor() + player.getLastKnownUsername();
        }
    }

    // TODO Refactor to use FeatherPubSub
    public static class FriendsRedisListener implements RedisListener {

        @Override
        public void receive(String channel, JSONObject object) {
            if ("friend-requests".equalsIgnoreCase(channel)) {
                UUID from = UUID.fromString((String) object.get("from"));
                UUID target = UUID.fromString((String) object.get("target"));

                Player p = Bukkit.getPlayer(target);
                if (p != null) {
                    NetworkPlayer np = NetworkPlayer.of(p);

                    if (np == null) {
                        return;
                    }

                    String name = getNameFromUuid(from);
                    HAS_REQUESTED_FRIENDSHIP.sendMessage(p, name);

                    np.addFriendRequest(from);

                    ComponentBuilder builder = new ComponentBuilder("Click to reply").color(ChatColor.GRAY);
                    builder.append(" [ACCEPT]").color(ChatColor.GREEN).bold(true);
                    builder.event(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/friend accept " + ChatColor.stripColor(name)));
                    builder.append(" [DENY]").color(ChatColor.RED).bold(true);
                    builder.event(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/friend deny " + ChatColor.stripColor(name)));

                    p.spigot().sendMessage(builder.create());
                }
            } else if ("friend-acceptances".equalsIgnoreCase(channel)) {
                UUID who = UUID.fromString((String) object.get("who"));
                UUID from = UUID.fromString((String) object.get("from"));

                Player p = Bukkit.getPlayer(from);
                if (p != null) {
                    ACCEPTED_REQUEST.sendMessage(p, getNameFromUuid(who));
                }
            } else if ("friend-denials".equalsIgnoreCase(channel)) {
                UUID who = UUID.fromString((String) object.get("who"));
                UUID from = UUID.fromString((String) object.get("from"));

                Player p = Bukkit.getPlayer(from);
                if (p != null) {
                    OTHER_DENIED_REQUEST.sendMessage(p, getNameFromUuid(who));
                }
            } else if ("friend-backstabs".equalsIgnoreCase(channel)) {
                UUID who = UUID.fromString((String) object.get("who"));
                UUID from = UUID.fromString((String) object.get("target"));

                Player p = Bukkit.getPlayer(from);
                if (p != null) {
                    NO_LONGER_FRIENDS.sendMessage(p, getNameFromUuid(who));
                }
            }
        }
    }

    @Getter
    private static class FriendData {
        private final UUID uuid;
        private String name;

        private ServerType serverType;
        private GameType gameType;
        private String server;

        FriendData(UUID uuid, String server) {
            this.uuid = uuid;
            this.name = getNameFromUuid(uuid);
            this.server = server;

            this.serverType = ServerType.getType(server);

            if (serverType == ServerType.GAME) {
                this.gameType = GameType.getByServerId(server);
            }
        }

        FriendData(UUID uuid, String name, String server) {
            this.uuid = uuid;
            this.name = name;
            this.server = server;

            if (server == null) return;

            serverType = ServerType.getType(server);

            if (serverType == ServerType.GAME) {
                gameType = GameType.getByServerId(server);

                if (gameType == null) {
                    serverType = ServerType.UNKNOWN;
                }
            }
        }

        public boolean isOnline() {
            return server != null;
        }

        public String getRawName() {
            if (name == null) {
                return "";
            }

            return ChatColor.stripColor(name);
        }

        public String getStatus() {
            if (!isOnline()) {
                return "offline";
            }

            switch (serverType) {
                case HUB:
                    return "in a Hub";
                case GAME:
                    return "playing " + gameType.getDisplayName();
            }

            return "in an unknown place";
        }

        public boolean canJoinServer(NetworkPlayer player) {
            return isOnline() && serverType == ServerType.HUB || (gameType != null && gameType.canJoin(player));
        }
    }


    private static class OfflineFriendData extends FriendData {
        public OfflineFriendData(UUID uuid, String name) {
            super(uuid, name, null);
        }

        public OfflineFriendData(UUID uuid) {
            this(uuid, getNameFromUuid(uuid));
        }

        @Override
        public boolean isOnline() {
            return false;
        }

        @Override
        public boolean canJoinServer(NetworkPlayer player) {
            return false;
        }
    }

}

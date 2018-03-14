package net.minespree.feather;

import com.mongodb.client.MongoCollection;
import lombok.Getter;
import net.minespree.analytics.AnalyticsClient;
import net.minespree.babel.Babel;
import net.minespree.feather.announcement.AnnouncementCommandListener;
import net.minespree.feather.announcement.AnnouncementManager;
import net.minespree.feather.command.AdminDevCommands;
import net.minespree.feather.command.BuycraftCommands;
import net.minespree.feather.command.EssentialCommands;
import net.minespree.feather.command.FriendCommands;
import net.minespree.feather.command.MessageCommands;
import net.minespree.feather.command.PunishmentCommands;
import net.minespree.feather.command.StaffCommands;
import net.minespree.feather.command.system.CommandManager;
import net.minespree.feather.data.chat.ChatManager;
import net.minespree.feather.data.chat.PrivateMessagingRedisListener;
import net.minespree.feather.data.damage.impl.CombatTracker;
import net.minespree.feather.data.gamedata.perks.PerkHandler;
import net.minespree.feather.data.games.GameType;
import net.minespree.feather.data.games.ServerType;
import net.minespree.feather.data.tab.TabManager;
import net.minespree.feather.data.update.UpdateBook;
import net.minespree.feather.data.whitelist.Whitelist;
import net.minespree.feather.db.mongo.MongoManager;
import net.minespree.feather.db.redis.RedisManager;
import net.minespree.feather.listener.PlayerChatListener;
import net.minespree.feather.listener.PlayerListener;
import net.minespree.feather.listener.TwitterSignListener;
import net.minespree.feather.player.PlayerManager;
import net.minespree.feather.player.PlayerRedisListener;
import net.minespree.feather.player.PlayerTracker;
import net.minespree.feather.player.achievements.PlayerAchievements;
import net.minespree.feather.player.nick.NickManager;
import net.minespree.feather.player.perks.PlayerPerks;
import net.minespree.feather.player.settings.PlayerSettings;
import net.minespree.feather.queue.PartyJoinManager;
import net.minespree.feather.queue.QueueJoiner;
import net.minespree.feather.staff.StaffChat;
import net.minespree.feather.util.RestartManager;
import net.minespree.wizard.executors.BukkitAsyncExecutor;
import org.bson.Document;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;

@Getter
public class FeatherPlugin extends JavaPlugin {
    private ChatManager chatManager;
    private StaffChat staffChat;
    private PlayerTracker playerTracker;
    private NickManager nickManager;
    private QueueJoiner queueJoiner;
    private UpdateBook updateBook;
    private PartyJoinManager partyJoinManager;
    private RestartManager restartManager;

    private BukkitAsyncExecutor asyncExecutor;

    private ServerType serverType;
    private GameType gameType;

    public static FeatherPlugin get() {
        return JavaPlugin.getPlugin(FeatherPlugin.class);
    }

    @Override
    public void onEnable() {
        File file = new File(getDataFolder(), "config.yml");
        if (!file.exists()) {
            saveDefaultConfig();
        }

        String serverId = getServerId();
        serverType = ServerType.getType(serverId);

        if (serverType == ServerType.GAME) {
            gameType = GameType.getByServerId(serverId);
        }

        getServer().getMessenger().registerOutgoingPluginChannel(this, "BungeeCord");

        RedisManager.getInstance().init(
                getConfig().getString("Redis.Host"),
                getConfig().getInt("Redis.Port")
        );
        MongoManager.getInstance().createConnection(
                getConfig().getString("Mongo.Host"),
                getConfig().getInt("Mongo.Port"),
                getConfig().getString("Mongo.Database"),
                getConfig().getString("Mongo.User"), getConfig().getString("Mongo.Pass")
        );

        AnalyticsClient.createClient(getConfig().getString("Analytics.GameKey"), getConfig().getString("Analytics.SecretKey"));
        AnalyticsClient.getClient().setBuild("Bukkit");

        PlayerManager.getInstance().startTask();

        asyncExecutor = BukkitAsyncExecutor.create(this);

        new Whitelist(this);

        PlayerManager.getInstance().init();
        PlayerManager.getInstance().startTask();
        CommandManager.getInstance();

        // Get babel collection and load all strings
        MongoCollection<Document> babelCollection = MongoManager.getInstance().getCollection("babel");
        Babel.initialize(babelCollection);

        PlayerAchievements.init();
        PlayerSettings.init();
        PlayerPerks.init();
        TabManager.getInstance().load();
        AnnouncementManager.getInstance().load();

        chatManager = new ChatManager();
        queueJoiner = new QueueJoiner(RedisManager.getInstance());

        getServer().getPluginManager().registerEvents(new PlayerListener(), this);
        getServer().getPluginManager().registerEvents(new PlayerChatListener(), this);
        getServer().getPluginManager().registerEvents(new TwitterSignListener(), this);

        staffChat = new StaffChat();
        staffChat.hook();
        nickManager = new NickManager();

        CommandManager.getInstance().registerClass(EssentialCommands.class);
        PunishmentCommands.hookRedisListener();
        CommandManager.getInstance().registerClass(PunishmentCommands.class);
        CommandManager.getInstance().registerClass(BuycraftCommands.class);
        CommandManager.getInstance().registerClass(FriendCommands.class);
        AdminDevCommands.registerEnum();
        CommandManager.getInstance().registerClass(AdminDevCommands.class);
        CommandManager.getInstance().registerClass(StaffCommands.class);

        RedisManager.getInstance().registerListener(new FriendCommands.FriendsRedisListener(), "friend-requests", "friend-acceptances", "friend-denials", "friend-backstabs");

        CombatTracker.hook();

        PlayerRedisListener listener = new PlayerRedisListener();
        RedisManager.getInstance().registerListener("player-join", listener);
        RedisManager.getInstance().registerListener("player-quit", listener);

        playerTracker = new PlayerTracker();
        RedisManager.getInstance().registerListener("private_messages", new PrivateMessagingRedisListener());
        CommandManager.getInstance().registerClass(MessageCommands.class);

        RedisManager.getInstance().registerListener("announce", new AnnouncementCommandListener());

        this.updateBook = new UpdateBook();
        this.restartManager = new RestartManager();

        PerkHandler.getInstance().load();
    }

    public void setPartyJoinManager(PartyJoinManager manager) {
        this.partyJoinManager = manager;
    }

    public String getServerId() {
        // PlayPen changes this info on startup
        return getServer().getServerName();
    }
}

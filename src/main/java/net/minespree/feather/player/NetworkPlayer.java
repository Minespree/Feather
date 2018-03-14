package net.minespree.feather.player;

import com.google.common.net.InetAddresses;
import lombok.Getter;
import lombok.Setter;
import net.md_5.bungee.api.chat.BaseComponent;
import net.minespree.feather.achievements.Achievement;
import net.minespree.feather.crates.CrateManager;
import net.minespree.feather.crates.PlayerCrateManager;
import net.minespree.feather.data.gamedata.perks.Perk;
import net.minespree.feather.experience.Experience;
import net.minespree.feather.player.achievements.PlayerAchievementManager;
import net.minespree.feather.player.achievements.PlayerAchievements;
import net.minespree.feather.player.perks.PlayerPerkManager;
import net.minespree.feather.player.perks.PlayerPerks;
import net.minespree.feather.player.rank.Rank;
import net.minespree.feather.player.settings.PlayerSettingManager;
import net.minespree.feather.player.settings.PlayerSettings;
import net.minespree.feather.player.stats.persitent.PersistentStatistics;
import net.minespree.feather.settings.FeatherSettings;
import net.minespree.feather.settings.Setting;
import net.minespree.feather.util.BSONUtil;
import org.bson.Document;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.lang.ref.WeakReference;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

@Getter
public class NetworkPlayer extends FeatherBasePlayer {
    private WeakReference<Player> player;

    private PersistentStatistics stats;
    private PlayerSettingManager settings;
    private PlayerAchievementManager achievements;
    private PlayerPerkManager perks;
    private CrateManager crateManager;

    @Setter
    private UUID lastMessager;

    @Setter
    private ChatColor overrideColor;

    private volatile boolean loaded = false;

    public NetworkPlayer(UUID uuid) {
        super(uuid);
    }

    public static NetworkPlayer of(Player player) {
        return PlayerManager.getInstance().getPlayer(player);
    }

    @Override
    public void load(Document document, boolean bootstrap) {
        // Load base variables
        String rankName = document.getString(PlayerKey.RANK);
        this.rank = Rank.byName(rankName);

        Long firstJoin = document.getLong(PlayerKey.FIRST_JOIN);
        this.firstJoin = firstJoin == null ? System.currentTimeMillis() : firstJoin;

        // Don't update last join date if not requested
        if (bootstrap) {
            this.lastJoin = System.currentTimeMillis();
        }

        this.coins = document.getInteger(PlayerKey.COINS, 0);
        this.gems = document.getInteger(PlayerKey.GEMS, 0);

        this.level = document.getInteger(PlayerKey.LEVEL, 1);

        Long experience = document.getLong(PlayerKey.EXPERIENCE);
        this.experience = experience == null ? 0L : experience;

        this.twoFaSecret = BSONUtil.wrapEmptyString(document, PlayerKey.TWO_FA_SECRET);
        this.lastKnownUsername = document.getString(PlayerKey.LAST_NAME);
        this.nick = BSONUtil.wrapEmptyString(document, PlayerKey.NICKNAME);

        String nickRankName = document.getString(PlayerKey.NICKED_RANK);

        if (nickRankName != null) {
            this.nickedRank = Rank.byName(nickRankName);
        }

        this.claimedAlpha = document.getBoolean(PlayerKey.CLAIMED_ALPHA_CODES, false);
        this.prefix = BSONUtil.wrapEmptyString(document, PlayerKey.PREFIX);
        this.monthlyRank = document.getBoolean(PlayerKey.MONTHLY_RANK, false);
        this.updateVersion = document.getInteger(PlayerKey.UPDATE_VERSION, -1);

        // Load complex variables
        this.knownNames = BSONUtil.stringListToSet(document, PlayerKey.KNOWN_NAMES);
        this.knownIps = BSONUtil.stringListToSet(document, PlayerKey.KNOWN_IPS);
        this.purchasedPackages = BSONUtil.stringListToSet(document, PlayerKey.PURCHASED_PACKAGES);
        this.ignoredPlayers = BSONUtil.uuidListToSet(document, PlayerKey.IGNORED_PLAYERS);

        loadSettings(document);
        loadAchievements(document);
        loadCrates(document);
        loadPerks(document);

        if (bootstrap) {
            bootstrap(document);
        }

        loaded = true;
    }

    /**
     * Method called after a player is loaded. Should be used if another plugin
     * needs data from the main player's {@link Document} stored in MongoDB.
     * Won't get called if this player was loaded through {@link net.minespree.feather.player.loaders.PlayerLoader#loadPlayer(UUID, Supplier, boolean)}
     * with {@code bootstrap} set to {@code false}
     */
    @Override
    public void bootstrap(Document document) {
    }

    private void loadSettings(Document document) {
        Document settingsDoc = BSONUtil.getSubDoc(document, PlayerKey.SETTINGS);

        this.settings = PlayerSettings.createManager(this);
        settings.loadData(PlayerSettings.getRegistry(), settingsDoc);
    }

    private void loadAchievements(Document document) {
        Document achievevementsDoc = BSONUtil.getSubDoc(document, PlayerKey.ACHIEVEMENTS);

        this.achievements = PlayerAchievements.createManager(this);

        achievements.loadData(PlayerAchievements.getRegistry(), achievevementsDoc);
    }

    private void loadCrates(Document document) {
        List<Document> crateList = BSONUtil.getSubDocList(document, PlayerKey.CRATES);

        this.crateManager = new PlayerCrateManager(this, crateList);
    }

    private void loadPerks(Document document) {
        Document perksDoc = BSONUtil.getSubDoc(document, PlayerKey.PERKS);

        this.perks = PlayerPerks.createManager(this);
        perks.loadData(PlayerPerks.getRegistry(), perksDoc);
        perks.loadSets(document);
    }

    private void addKnownIp(String address) {
        if (knownIps.add(address)) {
            addSetUpdate(PlayerKey.KNOWN_IPS, address);
        }
    }

    private void addKnownName(String name) {
        if (knownNames.add(name)) {
            addSetUpdate(PlayerKey.KNOWN_NAMES, name);
        }
    }

    public Player getPlayer() {
        if (!loaded || player == null) {
            return null;
        }

        return player.get();
    }

    @Override
    public void setPlayer(Player player) {
        Player old = getPlayer();

        if (old != null && old.isOp()) {
            old.setOp(false);
        }

        this.player = new WeakReference<>(player);

        if (player != null) {
            if (player.isOnline()) {
                String address = InetAddresses.toAddrString(player.getAddress().getAddress());
                addKnownIp(address);
            }

            String name = player.getName();

            addKnownName(name);
            addUpdate(PlayerKey.LAST_NAME, name);
            addUpdate(PlayerKey.LAST_JOIN, this.lastJoin);

            Rank rank = getRank();

            if (rank != null && rank.has(Rank.ADMIN)) {
                player.setOp(true);
            }
        }
    }

    public void updateTag() {
        Player player = getPlayer();

        if (player == null) return;

        String name = player.getName();

        if (hasNick()) {
            Rank rank = getNickedRank();

            player.setDisplayName(rank.getColoredTag() + getNick());
        } else if (prefix != null) {
            player.setDisplayName(prefix + name);
        } else {
            player.setDisplayName(this.rank.getColoredTag() + name);
        }
    }

    @Override
    public String getName() {
        Player player = getPlayer();

        if (player == null) {
            return "Player";
        }

        return hasNick() ? getNick() : player.getName();
    }

    @Override
    public void setRank(Rank rank) {
        if (this.rank == rank) return;

        this.rank = rank;
        addUpdate(PlayerKey.RANK, rank.name());

        updateTag();
    }

    @Override
    public void addExperience(long amount, String reason) {
        boolean levelUp = addExperience(amount);
        Player player = getPlayer();

        if (player == null) return;

        Experience.sendMessage(this, amount, reason);

        if (levelUp) {
            Experience.sendLevelUp(this, this.level);
        }
    }

    @Override
    public void sendMessage(BaseComponent[] components) {
        Player player = getPlayer();

        if (player != null && player.isOnline()) {
            player.spigot().sendMessage(components);
        }
    }

    @Override
    public void sendMessage(String message) {
        Player player = getPlayer();

        if (player != null && player.isOnline()) {
            player.sendMessage(message);
        }
    }

    @Override
    public boolean setAchievement(Achievement achievement, Object value) {
        boolean changed = achievements.setValue(achievement, value, true);

        if (changed) {
            addUpdate(PlayerKey.ACHIEVEMENTS + "." + achievement.getId(), achievement.getType().toString(value));
        }

        return changed;
    }

    @Override
    public boolean setSetting(Setting setting, Object value) {
        boolean changed = settings.setValue(setting, value, true);

        if (changed) {
            addUpdate(PlayerKey.SETTINGS + "." + setting.getId(), setting.getType().toString(value));
        }

        return changed;
    }

    @Override
    public boolean setPerk(Perk perk, int level) {
        boolean changed = perks.setValue(perk, level, true);

        if (changed) {
            addUpdate(PlayerKey.PERKS + "." + perk.getId(), level);
        }

        return changed;
    }

    @Override
    public void setNick(String nick, Rank nickedRank) {
        this.nick = nick;
        this.nickedRank = nickedRank;

        addUpdate(PlayerKey.NICKNAME, nick);
        addUpdate(PlayerKey.NICKED_RANK, nickedRank.name());

        updateTag();
    }

    @Override
    public String colorName() {
        Player player = getPlayer();

        if (player == null) {
            return rank.getColor() + "Player";
        }

        String name = hasNick() ? getNick() : player.getName();

        if (overrideColor != null) {
            return overrideColor + name;
        } else {
            return rank.getColor() + name;
        }
    }

    @Override
    public void setPrefix(String prefix) {
        this.prefix = prefix;
        addUpdate(PlayerKey.PREFIX, prefix);

        updateTag();
    }

    @Override
    public PersistentStatistics getPersistentStats() {
        if (stats == null) {
            stats = new PersistentStatistics(uuid);
        }

        return stats;
    }

    @Override
    public boolean sendAnnouncements() {
        return (boolean) settings.getValue(FeatherSettings.ANNOUNCEMENTS);
    }

    @Override
    public boolean staffChatEnabled() {
        return rank.has(Rank.HELPER) && (boolean) settings.getValue(FeatherSettings.STAFF_CHAT);
    }

    /**
     * @deprecated Move to new settings system
     */
    @Deprecated
    public boolean isStaffChat() {
        return (boolean) settings.getValue(FeatherSettings.STAFF_CHAT);
    }

    /**
     * @deprecated Move to new settings system
     */
    @Override
    @Deprecated
    public void setStaffChat(boolean enabled) {
        // TODO Remove notifications from actual command. Global callbacks already handle this
        setSetting(FeatherSettings.STAFF_CHAT, enabled);
    }
}

package net.minespree.feather.player;

import net.md_5.bungee.api.chat.BaseComponent;
import net.minespree.feather.achievements.Achievement;
import net.minespree.feather.crates.CrateManager;
import net.minespree.feather.data.gamedata.perks.Perk;
import net.minespree.feather.player.rank.Rank;
import net.minespree.feather.player.save.Saveable;
import net.minespree.feather.player.stats.persitent.PersistentStatistics;
import net.minespree.feather.settings.Setting;
import org.bson.Document;
import org.bukkit.entity.Player;

import java.util.Collection;
import java.util.Set;
import java.util.UUID;

public interface BasePlayer extends Saveable {
    UUID getUuid();

    void setPlayer(Player player);

    boolean isLoaded();

    void load(Document document, boolean bootstrap);

    void bootstrap(Document document);

    Rank getRank();

    void setRank(Rank rank);

    boolean isMonthlyRank();

    void setMonthlyRank(boolean monthly);

    Set<String> getKnownNames();

    Set<String> getKnownNamesLowercase();

    Set<String> getKnownIps();

    Set<String> getPurchasedPackages();

    Set<UUID> getIgnoredPlayers();

    PersistentStatistics getPersistentStats();

    CrateManager getCrateManager();

    String getName();

    String getLastKnownUsername();

    long getLastJoin();

    long getFirstJoin();

    int getCoins();

    void setCoins(int coins);

    default void addCoins(int coins) {
        setCoins(getCoins() + coins);
    }

    default void removeCoins(int coins) {
        int balance = getCoins() - coins;

        setCoins(balance < 0 ? 0 : balance);
    }

    int getGems();

    void setGems(int gems);

    default void addGems(int gems) {
        setGems(getGems() + gems);
    }

    default void removeGems(int gems) {
        int balance = getGems() - gems;

        setGems(balance < 0 ? 0 : balance);
    }

    int getLevel();

    long getExperience();

    float getNeededZeroToOne();

    /**
     * @return if the player has leveled up
     */
    boolean addExperience(long amount);

    void addExperience(long amount, String reason);

    void sendMessage(BaseComponent[] components);

    void sendMessage(String message);

    boolean sendAnnouncements();

    boolean setAchievement(Achievement achievement, Object value);

    boolean setSetting(Setting setting, Object value);

    boolean setPerk(Perk perk, int level);

    String getTwoFaSecret();

    default boolean hasTwoFa() {
        return getTwoFaSecret() != null;
    }

    // TODO Fix naming inconsistency
    void set2FA(String secret);

    String getNick();

    default boolean hasNick() {
        return getNick() != null;
    }

    void setNick(String nick, Rank nickedRank);

    String colorName();

    String getPrefix();

    void setPrefix(String prefix);

    boolean isMuted();

    void setMuted(long till);

    long getMutedTill();

    void setFriends(Set<UUID> uuids);

    void addFriend(UUID uuid);

    void removeFriend(UUID uuid);

    void addFriendRequest(UUID uuid);

    void removeFriendRequest(UUID uuid);

    boolean hasFriendRequestFrom(UUID uuid);

    Collection<UUID> getFriendRequests();

    int getUpdateVersion();

    void setUpdateVersion(int updateVersion);

    boolean staffChatEnabled();

    void setStaffChat(boolean enabled);
}

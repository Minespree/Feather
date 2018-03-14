package net.minespree.feather.player;

import com.google.common.base.Preconditions;
import com.google.common.collect.LinkedListMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import lombok.Getter;
import net.minespree.feather.experience.ExperienceChangeEvent;
import net.minespree.feather.player.rank.Rank;
import org.bukkit.Bukkit;

import java.util.LinkedHashMap;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Getter
public abstract class FeatherBasePlayer implements BasePlayer {
    protected final UUID uuid;
    protected final Set<UUID> friendRequests = Sets.newHashSet();
    protected final LinkedHashMap<String, Object> saveQueue = new LinkedHashMap<>();
    protected final Multimap<String, Object> saveSetQueue = LinkedListMultimap.create();
    protected final LinkedHashMap<String, Object> saveOperations = new LinkedHashMap<>();
    protected Rank rank;
    protected Set<String> knownNames;
    protected Set<String> knownIps;
    protected Set<String> purchasedPackages;
    protected Set<UUID> friends;
    protected Set<UUID> ignoredPlayers;
    protected String lastKnownUsername;
    protected long lastJoin;
    protected long firstJoin;
    protected int coins;
    protected int gems;
    protected int level;
    protected long experience;
    protected String twoFaSecret;
    protected String nick;
    protected Rank nickedRank;
    protected boolean claimedAlpha;
    protected boolean monthlyRank;
    protected String prefix;
    protected boolean muted;
    protected long mutedTill;
    protected int updateVersion;
    protected long lastSave;

    public FeatherBasePlayer(UUID uuid) {
        Preconditions.checkNotNull(uuid);
        this.uuid = uuid;
    }

    @Override
    public Set<String> getKnownNamesLowercase() {
        return knownNames.stream()
                .map(String::toLowerCase)
                .collect(Collectors.toSet());
    }

    @Override
    public void setMonthlyRank(boolean monthly) {
        this.monthlyRank = monthly;
        addUpdate(PlayerKey.MONTHLY_RANK, monthly);
    }

    @Override
    public void setCoins(int coins) {
        this.coins = coins;
        addUpdate(PlayerKey.COINS, coins);
    }

    @Override
    public void setGems(int gems) {
        this.gems = gems;
        addUpdate(PlayerKey.COINS, gems);
    }

    @Override
    public float getNeededZeroToOne() {
        long current = experience;
        long needed = 200 + (1000 * (level - 1));

        return (float) current / needed;
    }

    @Override
    public boolean addExperience(long amount) {
        long needed = 200 + (1000 * (level - 1));
        long oldExp = this.experience;
        int oldLevel = this.level;

        experience += amount;
        boolean levelUp = experience >= needed;

        if (levelUp) {
            level++;
            experience = 0;

            addUpdate(PlayerKey.LEVEL, level);
        }

        addUpdate(PlayerKey.EXPERIENCE, experience);

        if (this instanceof NetworkPlayer) {
            Bukkit.getPluginManager().callEvent(new ExperienceChangeEvent((NetworkPlayer) this, oldExp, oldLevel));
        }

        return levelUp;
    }

    @Override
    public void set2FA(String secret) {
        addUpdate(PlayerKey.TWO_FA_SECRET, secret);
    }

    @Override
    public void setMuted(long till) {
        Preconditions.checkArgument(till < System.currentTimeMillis(), "Cannot mute player in the past");

        muted = true;
        mutedTill = till;
    }

    @Override
    public void setFriends(Set<UUID> uuids) {
        this.friends = uuids;
    }

    @Override
    public void addFriend(UUID uuid) {
        friends.add(uuid);
    }

    @Override
    public void removeFriend(UUID uuid) {
        friends.remove(uuid);
    }

    @Override
    public void addFriendRequest(UUID uuid) {
        friendRequests.add(uuid);
    }

    @Override
    public void removeFriendRequest(UUID uuid) {
        friendRequests.remove(uuid);
    }

    @Override
    public boolean hasFriendRequestFrom(UUID uuid) {
        return friendRequests.contains(uuid);
    }

    @Override
    public void setUpdateVersion(int updateVersion) {
        this.updateVersion = updateVersion;
        addUpdate(PlayerKey.UPDATE_VERSION, updateVersion);
    }

    @Override
    public void saved() {
        lastSave = System.currentTimeMillis();
        saveQueue.clear();
        saveSetQueue.clear();
        saveOperations.clear();
    }

    @Override
    public void addUpdate(String key, Object value) {
        saveQueue.put(key, value);
    }

    @Override
    public void addSetUpdate(String key, Object valueToAdd) {
        saveSetQueue.put(key, valueToAdd);
    }

    @Override
    public void addSaveOperation(String key, Object document) {
        Preconditions.checkArgument(!key.equals("$set") && !key.equals("$addToSet"), "$set and $addToSet operations should use default update methods");
        saveOperations.put(key, document);
    }
}

package net.minespree.feather.player;

import net.minespree.feather.player.achievements.PlayerAchievements;
import net.minespree.feather.player.perks.PlayerPerks;
import net.minespree.feather.player.settings.PlayerSettings;

public class PlayerKey {
    public static final String RANK = "rank";
    public static final String FIRST_JOIN = "firstJoin";
    public static final String LAST_JOIN = "lastJoin";

    public static final String COINS = "coins";
    public static final String GEMS = "gems";

    public static final String LEVEL = "level";
    public static final String EXPERIENCE = "experience";
    public static final String LAST_NAME = "lastKnownName";
    public static final String TWO_FA_SECRET = "twoFaSecret";
    public static final String CLAIMED_ALPHA_CODES = "claimedAlphaCodes";
    public static final String NICKNAME = "nickName";
    public static final String NICKED_RANK = "nickedRank";
    public static final String MONTHLY_RANK = "monthlyRank";
    public static final String PREFIX = "prefix";
    public static final String UPDATE_VERSION = "updateVersion";

    public static final String KNOWN_NAMES = "knownNames";
    @Deprecated
    public static final String KNOWN_NAMES_LOWERCASE = "knownNamesLowercase";
    public static final String KNOWN_IPS = "knownIps";
    public static final String PURCHASED_PACKAGES = "purchasedPackages";
    public static final String IGNORED_PLAYERS = "ignoredPlayers";

    public static final String SETTINGS = PlayerSettings.MONGO_DOC;
    public static final String ACHIEVEMENTS = PlayerAchievements.MONGO_DOC;
    public static final String PERKS = PlayerPerks.MONGO_DOC;
    public static final String SETS = "sets";

    public static final String KITS = "kits";
    public static final String CRATES = "crates";
    public static final String UNLOCKED_COSMETICS = "unlockedCosmetics";
}

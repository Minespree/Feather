package net.minespree.feather.data.games;

import com.google.common.base.Preconditions;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import net.minespree.feather.player.NetworkPlayer;
import net.minespree.feather.player.rank.Rank;
import net.minespree.wizard.util.ItemBuilder;

@Getter
@AllArgsConstructor
public enum GameType {
    BLOCKWARS("blockwars", "BLOCKWARS", "BlockWars", "bw-"),
    SKYWARS("skywars", "SKYWARS", "SkyWars", "sw-"),
    THIMBLE("thimble", "THIMBLE", "Thimble", "th-"),
    CLASH("clash", "CLASH", "Clash", "cl-"),
    DOUBLE_TROUBLE("doubletrouble", "DOUBLE_TROUBLE", "Double Trouble", "dt-"),
    BATTLE_ROYALE("battleroyale", "BATTLE_ROYALE", "Battle Royale", "br-");

    /**
     * PlayPen's package name
     */
    private final String packageId;
    private final String databaseId;
    private final String displayName;
    private final String serverPrefix;

    @Setter
    private ItemBuilder item;

    private boolean maintenance;
    private boolean released;

    private Rank requiredRank;

    GameType(String packageId, String databaseId, String displayName, String serverPrefix, boolean maintenance, boolean released, Rank requiredRank) {
        this(packageId, databaseId, displayName, serverPrefix, null, maintenance, released, requiredRank);
    }

    GameType(String packageId, String databaseId, String displayName, String serverPrefix, Rank requiredRank) {
        this(packageId, databaseId, displayName, serverPrefix, false, true, requiredRank);
    }

    GameType(String packageId, String databaseId, String displayName, String serverPrefix) {
        this(packageId, databaseId, displayName, serverPrefix, Rank.MEMBER);
    }

    public static GameType getByServerId(String serverId) {
        Preconditions.checkNotNull(serverId);

        for (GameType type : values()) {
            if (serverId.startsWith(type.getServerPrefix())) {
                return type;
            }
        }

        return null;
    }

    public static GameType getByDatabase(String databaseId) {
        Preconditions.checkNotNull(databaseId);

        for (GameType type : values()) {
            if (databaseId.equals(type.getDatabaseId())) {
                return type;
            }
        }

        return null;
    }

    public boolean canJoin(NetworkPlayer player) {
        if (player == null || maintenance || !released) {
            return false;
        }

        if (player.getRank() == null) {
            return false;
        }

        return requiredRank.has(player.getRank());
    }
}

package net.minespree.feather.data.games;

import com.google.common.base.Preconditions;
import lombok.AllArgsConstructor;
import lombok.Getter;
import net.minespree.feather.player.NetworkPlayer;

@Getter
@AllArgsConstructor
public enum ServerType {
    HUB("Hub", false),
    GAME("Game", true),
    UNKNOWN("Unknown", false, false);

    public static final String HUB_PREFIX = "hub";

    private final String displayName;
    private final boolean playing;
    private final boolean friendJoinable;

    ServerType(String displayName, boolean playing) {
        this(displayName, playing, true);
    }

    public static ServerType getType(String serverId) {
        Preconditions.checkNotNull(serverId);

        GameType type = GameType.getByServerId(serverId);

        if (type != null) {
            return GAME;
        }

        return serverId.startsWith(HUB_PREFIX) ? HUB : UNKNOWN;
    }

    public static boolean canJoin(NetworkPlayer player, String serverId) {
        Preconditions.checkNotNull(player);

        ServerType type = getType(serverId);

        switch (type) {
            case HUB:
                return true;
            case UNKNOWN:
                return false;
            case GAME:
                GameType gameType = GameType.getByServerId(serverId);

                if (gameType == null) {
                    return false;
                }

                return gameType.canJoin(player);
        }

        return false;
    }
}

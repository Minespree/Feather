package net.minespree.feather.data.gamedata;

import lombok.Getter;

import java.util.Objects;

@Deprecated
public class GameRegistry {

    // TODO: Other types of data will be stored in here.

    @Getter
    public enum Type {
        // Network games.
        BLOCKWARS("bw", "BlockWars"),
        THIMBLE("th", "Thimble"),
        BATTLE_ROYALE("br", "Battle Royale"),
        SKYWARS("sw", "SkyWars"),
        DOUBLE_TROUBLE("dt", "Double Trouble"),
        CLASH("cl", "Clash"),
        //Hub games
        KOTL("KOTL"),
        GOLF("Golf"),
        SPLEEF("Spleef"),
        // Other,
        GLOBAL("Global"),
        UNKNOWN(null, "God knowns where"),
        HUB("hub", "Hub");

        private String prefix;
        private String name;
        private boolean hub;

        Type(String prefix, String name) {
            this.prefix = prefix;
            this.name = name;
            this.hub = false;
        }

        Type(String name) {
            this.name = name;
            this.hub = true;
        }

        public static Type byId(String id) {
            for (Type type : values()) {
                if (type.name().equals(id)) {
                    return type;
                }
            }

            return null;
        }

        public static Type getByPrefix(String prefix) {
            for (Type type : values()) {
                if (Objects.equals(type.getPrefix(), prefix)) {
                    return type;
                }
            }

            return null;
        }

        public static Type getByServerId(String serverId, boolean unknown) {
            // Custom override
            if (serverId.startsWith("hub")) {
                return HUB;
            }

            String[] parts = serverId.split("-");

            if (parts.length == 0) {
                return unknown ? UNKNOWN : null;
            }

            Type type = getByPrefix(parts[0]);

            return type != null ? type : (unknown ? UNKNOWN : null);
        }
    }

}


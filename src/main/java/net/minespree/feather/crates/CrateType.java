package net.minespree.feather.crates;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.concurrent.TimeUnit;

@Getter
@AllArgsConstructor
public enum CrateType {
    DEFAULT("default", "crate_default", false),
    HALLOWEEN("halloween", "crate_halloween", TimeUnit.DAYS.toMillis(7), SeasonalEvent.HALLOWEEN),
    CHRISTMAS("christmas", "crate_christmas", TimeUnit.DAYS.toMillis(7), SeasonalEvent.CHIRSTMAS),
    EVENT("event", "crate_event", true);

    private static final long NO_EXPIRY = -1L;

    private String id;
    private String babel;

    /**
     * Number of milliseconds this crate type is allowed
     * to live after a season/event has ended
     */
    private long afterExpiry;

    private SeasonalEvent seasonalEvent;
    private boolean event;

    CrateType(String id, String babel, boolean event) {
        this(id, babel, NO_EXPIRY, null, event);
    }

    CrateType(String id, String babel, long afterExpiry, SeasonalEvent seasonalEvent) {
        this(id, babel, afterExpiry, seasonalEvent, false);
    }

    public static CrateType byId(String id) {
        for (CrateType type : values()) {
            if (type.getId().equals(id)) {
                return type;
            }
        }

        return null;
    }

    public boolean isSeasonal() {
        return seasonalEvent != null;
    }

    public boolean expires() {
        return afterExpiry != NO_EXPIRY;
    }
}

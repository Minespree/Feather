package net.minespree.feather.queue;

import com.google.common.collect.Maps;
import lombok.Getter;
import net.minespree.feather.events.PartyJoinEvent;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Getter
public class PartyJoin {
    private static final long TIMEOUT = TimeUnit.SECONDS.toMillis(5);

    private UUID ownerUuid;
    private Map<UUID, Boolean> uuids;
    private long timestamp;

    public PartyJoin(List<UUID> uuidList) {
        this.uuids = Maps.newHashMapWithExpectedSize(uuidList.size());

        uuidList.forEach(uuid -> {
            uuids.put(uuid, false);
        });

        this.timestamp = System.currentTimeMillis();
    }

    public PartyJoinEvent toEvent() {
        return new PartyJoinEvent(ownerUuid, uuids.keySet());
    }

    public boolean awaits(UUID uuid) {
        return uuids.containsKey(uuid);
    }

    /**
     * Returns whether the party has joined completely
     */
    public boolean notifyJoin(UUID uuid) {
        uuids.put(uuid, true);

        for (boolean joined : uuids.values()) {
            if (!joined) {
                return false;
            }
        }

        return true;
    }

    public boolean hasExpired() {
        return (System.currentTimeMillis() - timestamp) >= TIMEOUT;
    }


}

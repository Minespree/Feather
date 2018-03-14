package net.minespree.feather.events;

import java.util.UUID;

public class PlayerLeaveNetworkEvent extends NetworkEvent {
    private UUID uuid;

    public PlayerLeaveNetworkEvent(UUID uuid) {
        this.uuid = uuid;
    }

    public UUID getUuid() {
        return uuid;
    }
}

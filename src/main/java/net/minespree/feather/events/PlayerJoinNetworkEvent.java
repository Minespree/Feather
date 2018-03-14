package net.minespree.feather.events;

import java.util.UUID;

public class PlayerJoinNetworkEvent extends NetworkEvent {

    private UUID uuid;

    public PlayerJoinNetworkEvent(UUID uuid) {
        this.uuid = uuid;
    }

    public UUID getUuid() {
        return uuid;
    }
}

package net.minespree.feather.events;

import lombok.Getter;
import net.minespree.feather.player.NetworkPlayer;
import net.minespree.feather.player.PlayerManager;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

@Getter
public class PartyJoinEvent extends NetworkEvent {
    private final List<NetworkPlayer> players;

    public PartyJoinEvent(UUID ownerUuid, Collection<UUID> uuids) {
        this.players = uuids.stream()
                .map(PartyJoinEvent::getPlayer)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        // First player is owner
        this.players.add(0, getPlayer(ownerUuid));
    }

    private static NetworkPlayer getPlayer(UUID uuid) {
        return PlayerManager.getInstance().getPlayer(uuid);
    }

    public NetworkPlayer getPartyOwner() {
        return players.get(0);
    }
}

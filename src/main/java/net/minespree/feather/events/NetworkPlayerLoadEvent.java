package net.minespree.feather.events;

import lombok.AllArgsConstructor;
import lombok.Getter;
import net.minespree.feather.player.NetworkPlayer;

@Getter
@AllArgsConstructor
public class NetworkPlayerLoadEvent extends NetworkEvent {
    private NetworkPlayer player;

    public boolean isOnline() {
        return player.getPlayer() != null && player.getPlayer().isOnline();
    }
}

package net.minespree.feather.player.nick;

import net.minespree.feather.events.NetworkEvent;
import net.minespree.feather.player.NetworkPlayer;

public class NickChangeEvent extends NetworkEvent {

    private NetworkPlayer player;
    private Action action;

    public NickChangeEvent(NetworkPlayer player, Action action) {
        this.player = player;
        this.action = action;
    }

    public NetworkPlayer getPlayer() {
        return player;
    }

    public Action getAction() {
        return action;
    }

    public enum Action {
        NEW_NICK,
        RESET_NICK
    }

}

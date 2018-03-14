package net.minespree.feather.data.gamedata.kits.event;

import net.minespree.feather.data.gamedata.kits.Tier;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class TierSetEvent extends Event {

    private static final HandlerList handlerList = new HandlerList();

    private Player player;
    private Tier tier;

    public TierSetEvent(Player player, Tier tier) {
        this.player = player;
        this.tier = tier;
    }

    public static HandlerList getHandlerList() {
        return handlerList;
    }

    @Override
    public HandlerList getHandlers() {
        return handlerList;
    }

    public Player getPlayer() {
        return player;
    }

    public Tier getTier() {
        return tier;
    }
}
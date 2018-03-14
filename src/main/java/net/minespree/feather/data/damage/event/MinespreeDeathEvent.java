package net.minespree.feather.data.damage.event;

import lombok.Getter;
import lombok.Setter;
import net.minespree.feather.data.damage.objects.CombatTrackerEntry;
import net.minespree.feather.data.damage.objects.KillAssist;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import java.util.ArrayList;
import java.util.List;

public class MinespreeDeathEvent extends Event {

    private static final HandlerList handlerList = new HandlerList();

    private Player player;
    private CombatTrackerEntry life;
    private boolean descriptive = true;
    private boolean broadcast = true;
    private boolean showHearts = true;
    @Getter
    @Setter
    private boolean drops = false;
    private String appendedText;
    private List<KillAssist> assists = new ArrayList<>();

    public MinespreeDeathEvent(Player player, CombatTrackerEntry life) {
        this.player = player;
        this.life = life;
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

    public CombatTrackerEntry getLife() {
        return life;
    }

    public boolean descriptive() {
        return descriptive;
    }

    public boolean broadcast() {
        return broadcast;
    }

    public void setDescriptive(boolean descriptive) {
        this.descriptive = descriptive;
    }

    public void setBroadcast(boolean broadcast) {
        this.broadcast = broadcast;
    }

    public String getAppendedText() {
        return appendedText;
    }

    public void setAppendedText(String appendedText) {
        this.appendedText = appendedText;
    }

    public boolean hasAssists() {
        return getAssists().size() > 0;
    }

    public List<KillAssist> getAssists() {
        return assists;
    }

    public void setAssists(List<KillAssist> assists) {
        this.assists = assists;
    }

    public boolean isShowHearts() {
        return showHearts;
    }

    public void setShowHearts(boolean showHearts) {
        this.showHearts = showHearts;
    }
}
package net.minespree.feather.data.damage.objects;

import lombok.NonNull;
import org.bukkit.entity.Player;

public class KillAssist implements Comparable<KillAssist> {

    private Player player;
    private double damage;
    private int percent;

    public KillAssist(Player player, double damage, int percent) {
        this.player = player;
        this.damage = damage;
        this.percent = percent;
    }

    public Player getPlayer() {
        return player;
    }

    public double getDamage() {
        return damage;
    }

    public int getPercent() {
        return percent;
    }

    @Override
    public int compareTo(@NonNull KillAssist o) {
        if (o.getPercent() > getPercent()) {
            return -1;
        } else if (o.getPercent() < getPercent()) {
            return 1;
        }

        return 0;
    }
}

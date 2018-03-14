package net.minespree.feather.data.damage;

import net.minespree.feather.data.damage.objects.CombatTrackerEntry;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent;

public interface ICombatTracker {
    default void setLife(Player player, CombatTrackerEntry entry) {
        setEntry(player, entry);
    }

    void setEntry(Player player, CombatTrackerEntry entry);

    default CombatTrackerEntry getLife(Player player) {
        return getEntry(player);
    }

    CombatTrackerEntry getEntry(Player player);

    default void removeLife(Player player) {
        removeEntry(player);
    }

    void removeEntry(Player player);

    void drainDamage(Player player);

    void damage(Player player, double amount, String reason, EntityDamageEvent.DamageCause cause);
}
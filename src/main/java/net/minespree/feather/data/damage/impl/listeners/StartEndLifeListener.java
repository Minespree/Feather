package net.minespree.feather.data.damage.impl.listeners;

import net.minespree.feather.data.damage.impl.CombatTracker;
import net.minespree.feather.data.damage.objects.CombatTrackerEntry;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public class StartEndLifeListener implements Listener {
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        CombatTrackerEntry entry = CombatTracker.getInstance().getLife(event.getPlayer());
        entry.reset();
        entry.setStartTime(System.currentTimeMillis());
    }
}

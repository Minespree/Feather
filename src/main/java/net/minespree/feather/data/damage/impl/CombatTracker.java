package net.minespree.feather.data.damage.impl;

import net.minespree.feather.FeatherPlugin;
import net.minespree.feather.data.damage.ICombatTracker;
import net.minespree.feather.data.damage.impl.listeners.DamageLifeDeathListener;
import net.minespree.feather.data.damage.impl.listeners.DamageLifeListener;
import net.minespree.feather.data.damage.impl.listeners.StartEndLifeListener;
import net.minespree.feather.data.damage.objects.CombatTrackerEntry;
import net.minespree.feather.data.damage.objects.DamageInfo;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.plugin.Plugin;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class CombatTracker implements ICombatTracker {

    private static CombatTracker instance;

    private volatile boolean isHooked;

    private Map<UUID, CombatTrackerEntry> lives = new ConcurrentHashMap<>();

    public static CombatTracker getInstance() {
        if (instance == null) {
            synchronized (CombatTracker.class) {
                if (instance == null) {
                    instance = new CombatTracker();
                }
            }
        }
        return instance;
    }

    public static void hook() {
        CombatTracker instance = getInstance();
        synchronized (CombatTracker.class) {
            if (!instance.isHooked) {
                try {
                    Plugin plugin = FeatherPlugin.get();
                    Bukkit.getPluginManager().registerEvents(new DamageLifeDeathListener(), plugin);
                    Bukkit.getPluginManager().registerEvents(new DamageLifeListener(), plugin);
                    Bukkit.getPluginManager().registerEvents(new StartEndLifeListener(), plugin);
                } finally {
                    instance.isHooked = true;
                }
            }
        }
    }

    @Override
    public void setEntry(Player player, CombatTrackerEntry entry) {
        lives.put(player.getUniqueId(), entry);
    }

    @Override
    public CombatTrackerEntry getEntry(Player player) {
        return lives.computeIfAbsent(player.getUniqueId(), uuid -> new CombatTrackerEntry(player));
    }

    @Override
    public void removeEntry(Player player) {
        lives.remove(player.getUniqueId());
    }

    /**
     * Should be used when resetting a player
     *
     * @param player
     */
    @Override
    public void drainDamage(Player player) {
        getLife(player).getDamageList().clear();
        getLife(player).setLastDamage(null);
    }

    @Override
    public void damage(Player player, double amount, String reason, EntityDamageEvent.DamageCause cause) {
        CombatTrackerEntry life = getLife(player);

        DamageInfo info = new DamageInfo(reason, amount, cause, System.currentTimeMillis());

        life.addDamage(info);
        life.setLastDamage(info);

        player.damage(amount);
    }
}

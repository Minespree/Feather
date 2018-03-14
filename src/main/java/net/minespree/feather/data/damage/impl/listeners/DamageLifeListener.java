package net.minespree.feather.data.damage.impl.listeners;

import net.minespree.feather.data.damage.impl.CombatTracker;
import net.minespree.feather.data.damage.objects.CombatTrackerEntry;
import net.minespree.feather.data.damage.objects.DamageInfo;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;

public class DamageLifeListener implements Listener {

    @EventHandler(priority = EventPriority.MONITOR)
    public void onDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player)) {
            return;
        }

        if (event instanceof EntityDamageByEntityEvent) {
            return;
        }

        if (event.isCancelled()) {
            return;
        }

        Player player = (Player) event.getEntity();

        CombatTrackerEntry life = CombatTracker.getInstance().getLife(player);

        if (event.getCause() == EntityDamageEvent.DamageCause.FALL || event.getCause() == EntityDamageEvent.DamageCause.VOID || event.getCause() == EntityDamageEvent.DamageCause.LAVA || event.getCause() == EntityDamageEvent.DamageCause.FIRE || event.getCause() == EntityDamageEvent.DamageCause.FIRE_TICK) {
            DamageInfo info = new DamageInfo((Entity) null, event.getDamage(), event.getCause(), System.currentTimeMillis());
            life.addDamage(info);
            life.setLastDamage(info);
        }
    }

    @EventHandler
    public void onDamageByTnt(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player)) return;

        if (event.getDamager() instanceof TNTPrimed) {
            TNTPrimed tnt = (TNTPrimed) event.getDamager();

            if (tnt.getSource() instanceof Player) {
                Player damager = (Player) tnt.getSource();

                Player player = (Player) event.getEntity();

                CombatTrackerEntry life = CombatTracker.getInstance().getLife(player);
                DamageInfo info = new DamageInfo(damager, event.getDamage(), event.getCause(), System.currentTimeMillis());

                life.addDamage(info);
                life.setLastDamage(info);
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onDamageByEntity(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player)) {
            return;
        }

        if (event.isCancelled()) {
            return;
        }

        Player player = (Player) event.getEntity();

        if (event.getDamager() instanceof Projectile) {
            Projectile proj = (Projectile) event.getDamager();
            if (proj.getShooter() instanceof Player) {
                if (proj.getShooter().equals(player)) {
                    return; // Don't track self damage.
                }
            }
        }

        CombatTrackerEntry life = CombatTracker.getInstance().getLife(player);
        DamageInfo info = new DamageInfo(event.getDamager(), event.getDamage(), event.getCause(), System.currentTimeMillis());

        life.addDamage(info);
        life.setLastDamage(info);
    }

}
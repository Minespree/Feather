package net.minespree.feather.data.damage.objects;

import com.google.common.base.Preconditions;
import org.bukkit.entity.AnimalTamer;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.Tameable;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.projectiles.ProjectileSource;

import java.util.Optional;

public class DamageInfo {
    private final String reason;

    private final Entity damager;

    private final double damage;

    private final EntityDamageEvent.DamageCause cause;

    private final long time;

    private final ItemStack heldItem;

    public DamageInfo(Entity damager, double damage, EntityDamageEvent.DamageCause cause, long time) {
        this(damager, null, damage, cause, time);
    }

    public DamageInfo(String reason, double damage, EntityDamageEvent.DamageCause cause, long time) {
        this(null, reason, damage, cause, time);
    }

    private DamageInfo(Entity damager, String reason, double damage, EntityDamageEvent.DamageCause cause, long time) {
        this.damager = damager;
        this.reason = reason;
        this.damage = damage;
        this.cause = cause;
        this.time = time;
        LivingEntity entity;
        if (damager instanceof LivingEntity) {
            entity = (LivingEntity) damager;
        } else if (damager instanceof Projectile && ((Projectile) damager).getShooter() instanceof LivingEntity) {
            entity = (LivingEntity) ((Projectile) damager).getShooter();
        } else /* if (damager instanceof Tameable && ((Tameable) damager).getOwner() instanceof LivingEntity) {
            // Tbf, who gives a fuck about held items when your pet does all the dirty work
            entity = (LivingEntity) ((Tameable) damager).getOwner();
        } else */ {
            entity = null;
        }

        if (entity != null) {
            this.heldItem = entity.getEquipment().getItemInHand();
        } else {
            this.heldItem = null;
        }
    }

    /**
     * @return Amount of damage contained in this DamageInfo
     */
    public double getDamage() {
        return damage;
    }

    /**
     * @return The damager contained in this damage info
     */
    public Entity getDamager() {
        return damager;
    }

    public boolean isDamagerOfType(Class<?> subType) {
        return this.damager != null && subType.isInstance(this.damager);
    }

    public ProjectileSource getShooter() {
        Preconditions.checkState(this.isDamagerOfType(Projectile.class));
        return ((Projectile) this.damager).getShooter();
    }

    public AnimalTamer getOwner() {
        Preconditions.checkState(this.isDamagerOfType(Tameable.class));
        return ((Tameable) this.damager).getOwner();
    }

    public Optional<Player> getRelatedPlayer() {
        if (this.isDamagerOfType(Player.class)) {
            return Optional.of(this.damager).map(Player.class::cast);
        } else if (this.isDamagerOfType(Tameable.class)) {
            return Optional.of(getOwner()).filter(Player.class::isInstance).map(Player.class::cast);
        } else if (this.isDamagerOfType(Projectile.class)) {
            return Optional.of(getShooter()).filter(Player.class::isInstance).map(Player.class::cast);
        }
        return Optional.empty();
    }

    public boolean hasRelatedPlayer() {
        return getRelatedPlayer().isPresent();
    }

    public Optional<ItemStack> getHeldItem() {
        return Optional.ofNullable(this.heldItem);
    }

    /**
     * May be null
     *
     * @return Cause of damage.
     */
    public EntityDamageEvent.DamageCause getCause() {
        return cause;
    }

    /**
     * Gets the time in milliseconds when the damage was dealt
     *
     * @return time of damage
     */
    public long getTime() {
        return time;
    }

    @Override
    public String toString() {
        return "DamageInfo{" +
                "damager=" + damager +
                ", damage=" + damage +
                ", cause=" + cause +
                ", time=" + time +
                '}';
    }

    public String getReason() {
        return reason;
    }
}

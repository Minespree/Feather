package net.minespree.feather.data.damage.impl.listeners;

import com.google.common.collect.Lists;
import net.minespree.feather.data.damage.event.MinespreeDeathEvent;
import net.minespree.feather.data.damage.impl.CombatTracker;
import net.minespree.feather.data.damage.objects.CombatTrackerEntry;
import net.minespree.feather.data.damage.objects.DamageCauseMessage;
import net.minespree.feather.data.damage.objects.DamageInfo;
import net.minespree.feather.data.damage.objects.KillAssist;
import net.minespree.feather.player.NetworkPlayer;
import net.minespree.feather.util.OptionalConsumer;
import net.minespree.feather.util.TimeUtils;
import net.minespree.wizard.util.Chat;
import org.apache.commons.lang.WordUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.block.BlockState;
import org.bukkit.entity.AnimalTamer;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.ExperienceOrb;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.Tameable;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.projectiles.BlockProjectileSource;
import org.bukkit.projectiles.ProjectileSource;

import java.text.DecimalFormat;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.function.Supplier;

public class DamageLifeDeathListener implements Listener {
    private Function<Entity, String> ENTITY_NAME = source -> {
        if (source instanceof Player) {
            return NetworkPlayer.of((Player) source).colorName();
        } else if (source != null) {
            return source.getCustomName() != null ? source.getCustomName() : source.getName();
        }
        return "null";
    };

    private Function<ProjectileSource, String> PROJECTILE_SOURCE_NAME = source -> {
        if (source instanceof Entity) {
            return ENTITY_NAME.apply((Entity) source);
        } else if (source instanceof BlockProjectileSource) {
            BlockState state = ((BlockProjectileSource) source).getBlock().getState();
            return state instanceof InventoryHolder ? ((InventoryHolder) state).getInventory().getTitle() : "block";
        }
        return "unknown source";
    };

    private Function<AnimalTamer, String> TAMER_NAME = source -> {
        if (source instanceof Entity) {
            return ENTITY_NAME.apply((Entity) source);
        }
        return source.getName();
    };

    @EventHandler
    public void playerDeathEvent(PlayerDeathEvent event) {
        event.setDeathMessage(null);
        CombatTrackerEntry life = CombatTracker.getInstance().getLife(event.getEntity());

        if (life.getLastDamage() != null) {
            //Avoiding stupid mistakes
            if (life.getDamageList().size() > 0) {
                MinespreeDeathEvent deathEvent = new MinespreeDeathEvent(event.getEntity(), life);
                if (!TimeUtils.elapsed(life.getLastDamage().getTime(), TimeUnit.SECONDS.toMillis(12))) {
                    List<KillAssist> assists = life.getPossibleAssists();
                    if (assists.size() > 0) {
                        deathEvent.setAssists(assists);
                    }

                    if (deathEvent.broadcast()) {
                        Player player = event.getEntity();
                        NetworkPlayer bp = NetworkPlayer.of(player);
                        String appended = deathEvent.getAppendedText() == null ? "" : deathEvent.getAppendedText();

                        if (deathEvent.isShowHearts()) {
                            for (Player p : Bukkit.getOnlinePlayers()) {
                                if (p == event.getEntity()) {
                                    OptionalConsumer.of(deathEvent.getLife().getLastDamagingPlayer()).ifPresent(killer -> {
                                        DecimalFormat format = new DecimalFormat("#.#");
                                        String f = format.format(killer.getHealth());
                                        p.sendMessage(bp.colorName() + " " + ChatColor.GRAY + getSimpleMessage(life) + ChatColor.GRAY + appended + ChatColor.GRAY + " - " + Chat.YELLOW + f + " " + Chat.RED + "❤");
                                    }).orElse(() -> p.sendMessage(bp.colorName() + " " + ChatColor.GRAY + getSimpleMessage(life) + ChatColor.GRAY + appended + ChatColor.GRAY + "."));
                                } else {
                                    p.sendMessage(bp.colorName() + " " + ChatColor.GRAY + getSimpleMessage(life) + ChatColor.GRAY + appended + ChatColor.GRAY + ".");
                                }
                            }
                        } else {
                            Bukkit.broadcastMessage(bp.colorName() + " " + ChatColor.GRAY + getSimpleMessage(life) + ChatColor.GRAY + appended + ChatColor.GRAY + ".");
                        }
                    }
                }

                Location dropLocation = event.getEntity().getLocation().clone();
                Bukkit.getServer().getPluginManager().callEvent(deathEvent);
                if (deathEvent.isDrops()) {
                    int droppedXp = event.getDroppedExp();
                    event.getDrops().forEach(item -> dropLocation.getWorld().dropItemNaturally(dropLocation, item));
                    ExperienceOrb orb = (ExperienceOrb) dropLocation.getWorld().spawnEntity(dropLocation, EntityType.EXPERIENCE_ORB);
                    orb.setExperience(droppedXp);
                    event.getDrops().clear();
                    event.setDroppedExp(0);
                }
            }
        }
        life.reset();
    }

    private Supplier<Optional<DamageInfo>> withEntity(CombatTrackerEntry entry) {
        return () -> {
            for (DamageInfo info : Lists.reverse(entry.getDamageList())) {
                if (info.getDamager() != null) {
                    return Optional.of(info);
                }
            }
            return Optional.empty();
        };
    }

    private String getKiller(DamageInfo info) {
        String actor;
        if (info.isDamagerOfType(Projectile.class)) {
            actor = PROJECTILE_SOURCE_NAME.apply(info.getShooter());
        } else if (info.isDamagerOfType(Tameable.class)) {
            actor = TAMER_NAME.apply(info.getOwner()) + "'s " + ENTITY_NAME.apply(info.getDamager());
        } else {
            actor = ENTITY_NAME.apply(info.getDamager());
        }
        return actor;
    }

    public String getSimpleMessage(CombatTrackerEntry life) {
        String deathMessage;
        DamageInfo lastDamage = life.getLastDamage();
        Supplier<Optional<DamageInfo>> lastDamageByEntity = withEntity(life);

        if (lastDamage.getDamager() == null) {
            if (lastDamage.getReason() != null) {
                deathMessage = "was killed by " + lastDamage.getReason();
            } else {
                Optional<DamageInfo> info = lastDamageByEntity.get();
                EntityType type = info.map(DamageInfo::getDamager).map(Entity::getType).orElse(EntityType.UNKNOWN);
                deathMessage = getCauseMessage(lastDamage.getCause(), type, info.map(this::getKiller).orElse(""));
            }
        } else {
            deathMessage = getCauseMessage(lastDamage.getCause(), lastDamage.getDamager().getType(), this.getKiller(lastDamage));
        }

        return deathMessage;
    }

    private String getNameOfCause(EntityDamageEvent.DamageCause cause) {
        if (cause == EntityDamageEvent.DamageCause.CONTACT) {
            return "Cactus";
        } else if (cause == EntityDamageEvent.DamageCause.FIRE_TICK) {
            return "Fire";
        }

        return WordUtils.capitalizeFully(cause.name().replace('_', ' '));
    }

    private String getCauseMessage(EntityDamageEvent.DamageCause cause, EntityType type, String killer) {
        return DamageCauseMessage.fromCause(cause).getMessage(type, killer);
    }

}

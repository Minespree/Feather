package net.minespree.feather.data.damage.objects;

import org.bukkit.entity.EntityType;
import org.bukkit.event.entity.EntityDamageEvent;

import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

public enum DamageCauseMessage {
    CONTACT(killer -> "was pierced by a cactus" + getKiller(killer, () -> " by " + killer)),
    ENTITY_ATTACK(killer -> "was killed" + getKiller(killer, () -> " by " + killer)),
    PROJECTILE((type, killer) -> "was " + projectile(type) + getKiller(killer, () -> " by " + killer)),
    SUFFOCATION(killer -> "suffocated" + getKiller(killer, () -> " due to " + killer)),
    FALL(killer -> "took a dive" + getKiller(killer, () -> " with help from " + killer)),
    FIRE(killer -> killer.isEmpty() ? "got pushed into a fire by " + killer : "died in a fire"),
    FIRE_TICK(killer -> "forgot to stop, drop and roll"),
    MELTING(killer -> "melt like snow before the sun"),
    LAVA(killer -> killer.isEmpty() ? "tried to swim in lava" : "was pushed into lava by " + killer),
    DROWNING(killer -> "drowned" + getKiller(killer, () -> " whilst trying to escape " + killer)),
    BLOCK_EXPLOSION(killer -> "was blown to bits" + getKiller(killer, () -> " by " + killer)),
    ENTITY_EXPLOSION(killer -> "was blown to bits" + getKiller(killer, () -> " by " + killer)),
    VOID(killer -> killer.isEmpty() ? "forgot flying wasn't possible" : "got thrown into the void by " + killer),
    LIGHTNING(killer -> "got struck by lightning"),
    SUICIDE(killer -> "committed suicide"),
    STARVATION(killer -> "starved to death"),
    POISON(killer -> killer.isEmpty() ? "died of poison" : "succumbed to the poison administered by" + killer),
    MAGIC(killer -> "got blasted by magic" + getKiller(killer, () -> " by " + killer)),
    WITHER(killer -> "withered away"),
    FALLING_BLOCK(killer -> "got squashed" + getKiller(killer, () -> " with help from " + killer)),
    THORNS(killer -> "got pierced to death" + getKiller(killer, () -> " while hugging " + killer)),
    CUSTOM(killer -> "got killed" + getKiller(killer, () -> " by " + killer));

    private static final Map<EntityDamageEvent.DamageCause, DamageCauseMessage> MESSAGES;

    static {
        MESSAGES = Collections.unmodifiableMap(new EnumMap<EntityDamageEvent.DamageCause, DamageCauseMessage>(EntityDamageEvent.DamageCause.class) {{
            Arrays.stream(DamageCauseMessage.values()).forEach(dcm -> {
                put(dcm.cause, dcm);
            });
        }});
    }

    private final EntityDamageEvent.DamageCause cause;
    private final BiFunction<EntityType, String, String> messageProducer;

    DamageCauseMessage(Function<String, String> messageProducer) {
        this((type, killer) -> messageProducer.apply(killer));
    }

    DamageCauseMessage(BiFunction<EntityType, String, String> messageProducer) {
        this.cause = EntityDamageEvent.DamageCause.valueOf(name());
        this.messageProducer = messageProducer;
    }

    private static String getKiller(String killer, Supplier<String> suffix) {
        return killer.isEmpty() ? "" : suffix.get();
    }

    private static String projectile(EntityType projectile) {
        switch (projectile) {
            case SNOWBALL:
                return "snowballed";
            case SMALL_FIREBALL:
            case FIREBALL:
                return "fireballed";
            case ARROW:
                return "shot";
            default:
                return "shot";
        }
    }

    public static DamageCauseMessage fromCause(EntityDamageEvent.DamageCause cause) {
        return MESSAGES.getOrDefault(cause, CUSTOM);
    }

    public String getMessage(EntityType type, String killer) {
        return this.messageProducer.apply(type, killer);
    }
}

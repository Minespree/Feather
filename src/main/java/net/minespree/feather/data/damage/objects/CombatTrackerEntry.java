package net.minespree.feather.data.damage.objects;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import net.minespree.feather.util.TimeUtils;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class CombatTrackerEntry {
    private Player player;
    private long startTime, endTime;
    private List<DamageInfo> damageList;
    // TODO: get rid of extra fields, we only want lastDamage
    private DamageInfo lastDamage;

    /**
     * Creates the DamageLife for the specified entity
     *
     * @param entity Living entity to create the DamageLife for.
     */
    public CombatTrackerEntry(Player entity) {
        this.player = entity;
        this.damageList = Lists.newArrayList();
    }

    /**
     * Gets the player for the DamageLife
     *
     * @return Player of the Life.
     */
    public Player getPlayer() {
        return player;
    }

    /**
     * Start time is first set when the living entity joins the server in this case.
     *
     * @return Time of join in milliseconds.
     */
    public long getStartTime() {
        return startTime;
    }

    /**
     * Sets the start time to track.
     *
     * @param startTime a long which is when the damage is started to be tracked.
     */
    public void setStartTime(long startTime) {
        this.startTime = startTime;
    }

    /**
     * End time is set when the living entity dies.
     *
     * @return Time of death in milliseconds.
     */
    public long getEndTime() {
        return endTime;
    }

    /**
     * Sets the end time to end tracking.
     *
     * @param endTime a long which will be set to the death time.
     */
    public void setEndTime(long endTime) {
        this.endTime = endTime;
    }

    /**
     * @return All of the damage information the living entity dealt during his life-time.
     */
    public List<DamageInfo> getDamageList() {
        return damageList;
    }

    /**
     * @return The last damage the living entity dealt.
     */
    public DamageInfo getLastDamage() {
        return lastDamage;
    }

    /**
     * Sets the last damage.
     *
     * @param lastDamage DamageInfo containing the information about the last damage the living entity dealt.
     */
    public void setLastDamage(DamageInfo lastDamage) {
        this.lastDamage = lastDamage;
    }

    public Optional<Player> getLastDamagingPlayer() {
        for (DamageInfo info : Lists.reverse(getDamageList())) {
            if (info.getRelatedPlayer().isPresent()) {
                return info.getRelatedPlayer();
            }
        }

        return Optional.empty();
    }

    /**
     * Adds a DamageInfo to the list of damage information.
     *
     * @param info DamageInfo to be added to the list
     */
    public void addDamage(DamageInfo info) {
        damageList.add(info);
    }

    public List<KillAssist> getPossibleAssists() {
        if (damageList.size() == 0) return new ArrayList<>();
        List<KillAssist> assists = new ArrayList<>();
        List<DamageInfo> infos = new ArrayList<>();
        double totalDamage = 0;
        Player killer = getLastDamagingPlayer().orElse(null);
        for (DamageInfo info : damageList) {
            if (info.hasRelatedPlayer()) {
                if (Objects.equals(info.getRelatedPlayer().get().getUniqueId(), killer.getUniqueId())) continue;
                if (!TimeUtils.elapsed(info.getTime(), TimeUnit.SECONDS.toMillis(8))) {
                    infos.add(info);
                    totalDamage += info.getDamage();
                }
            }
        }

        // Last player is the killer, (s)he gets no assist points (but the full kill credit instead)
        if (infos.size() > 0)
            infos.remove(infos.size() - 1);

        Set<UUID> assistedPlayers = Sets.newHashSet();

        for (DamageInfo info : infos) {
            // This is a certain condition, otherwise hasRelatedPlayer would return false, causing the DamageInfo never to
            // be added.
            Player player = info.getRelatedPlayer().get();
            double damage = info.getDamage();
            int percentage = (int) ((damage / totalDamage) * 100);
            if (percentage > 25 && assistedPlayers.add(player.getUniqueId())) /* 25% is the threshold */ {
                assists.add(new KillAssist(player, damage, percentage));
            }
        }

        Collections.sort(assists);
        return assists;
    }

    public void reset() {
        this.damageList.clear();
        this.startTime = System.currentTimeMillis();
        this.endTime = -1;
        this.lastDamage = null;
    }
}

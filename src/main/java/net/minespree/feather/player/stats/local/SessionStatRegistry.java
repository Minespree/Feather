package net.minespree.feather.player.stats.local;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Lists;
import com.google.common.collect.Table;
import net.minespree.babel.BabelMessageType;
import net.minespree.feather.player.NetworkPlayer;
import net.minespree.feather.player.stats.persitent.PersistentStatistics;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

public class SessionStatRegistry {

    protected final Table<UUID, StatType, SessionStatData> stats = HashBasedTable.create();

    public void trigger(Player player, StatType stat) {
        getSessionStatData(player, stat).update();
    }

    public void update(Player player, StatType stat, Object score) {
        SessionStatData playerStat = getSessionStatData(player, stat);
        playerStat.add(score);
    }

    public SessionStatData getSessionStatData(Player player, StatType statType) {
        return getSessionStatData(player.getUniqueId(), statType);
    }

    public SessionStatData getSessionStatData(UUID id, StatType statType) {
        SessionStatData SessionStatData = stats.get(id, statType);
        if (SessionStatData == null) {
            SessionStatData = new SessionStatData();
            stats.put(id, statType, SessionStatData);
        }
        return SessionStatData;
    }

    public List<BabelMessageType> getMessages() {
        Map<StatType, List<Map.Entry<UUID, SessionStatData>>> statTypes = stats.columnMap().entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, entry -> new ArrayList<>(entry.getValue().entrySet())));
        statTypes.forEach((type, dataList) -> dataList.sort((o1, o2) -> type.getSorter().comparator.compare(o1.getValue(), o2.getValue())));

        List<BabelMessageType> messages = Lists.newArrayList();

        for (Map.Entry<StatType, List<Map.Entry<UUID, SessionStatData>>> entry : statTypes.entrySet()) {
            StatType type = entry.getKey();
            if (type.isIgnored())
                continue;
            List<Map.Entry<UUID, SessionStatData>> perPlayer = statTypes.get(type);
            Map.Entry<Player, SessionStatData> top = null;

            for (Map.Entry<UUID, SessionStatData> aPerPlayer : perPlayer) {
                UUID playerId = aPerPlayer.getKey();
                Player player = Bukkit.getPlayer(playerId);
                if (player != null && aPerPlayer.getValue().get() > 0L) {
                    top = new AbstractMap.SimpleEntry<>(player, aPerPlayer.getValue());
                    break;
                }
            }

            if (top == null) {
                continue;
            }

            SessionStatData statData = top.getValue();
            messages.add(type.getMessageTop().apply(statData, top.getKey()));
        }

        return messages;
    }

    @SuppressWarnings("unchecked")
    public void save(NetworkPlayer player) {
        if (!stats.containsRow(player.getUuid()))
            return;
        Map<StatType, SessionStatData> dataMap = stats.row(player.getUuid());
        PersistentStatistics persistentStats = player.getPersistentStats();
        for (StatType statType : dataMap.keySet()) {
            if (dataMap.containsKey(statType)) {
                SessionStatData statData = dataMap.get(statType);
                if (statType.getSaveConsumer() != null && statData.getData() != null) {
                    statType.getSaveConsumer().accept(player, statData.getData());
                }
            }
        }
        persistentStats.persist();
    }

    public enum Sorter {
        HIGHEST_SCORE((o1, o2) -> -Double.compare(o1.get(), o2.get())),
        LOWEST_SCORE(Comparator.comparingDouble(SessionStatData::get)),
        FASTEST(Comparator.comparingLong(SessionStatData::getFirstTriggerMs)),
        SLOWEST((o1, o2) -> -Long.compare(o1.getFirstTriggerMs(), o2.getFirstTriggerMs())),
        TRIGGER(HIGHEST_SCORE.comparator),
        GLOBAL(TRIGGER.comparator);

        private final Comparator<SessionStatData> comparator;

        Sorter(Comparator<SessionStatData> comparator) {
            this.comparator = comparator;
        }
    }
}

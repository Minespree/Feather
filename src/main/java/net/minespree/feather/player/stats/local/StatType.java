package net.minespree.feather.player.stats.local;

import lombok.AllArgsConstructor;
import lombok.Data;
import net.minespree.babel.Babel;
import net.minespree.babel.BabelMessageType;
import net.minespree.babel.ComplexBabelMessage;
import net.minespree.feather.data.gamedata.GameRegistry;
import net.minespree.feather.player.NetworkPlayer;
import org.bukkit.entity.Player;

import java.util.function.BiConsumer;
import java.util.function.BiFunction;

@AllArgsConstructor
@Data
public class StatType {

    private BiFunction<SessionStatData, Player, BabelMessageType> messageTop, messagePersonal;

    private String name;
    private SessionStatRegistry.Sorter sorter;
    private boolean ignored;
    private int coins, slot;
    private BiConsumer<NetworkPlayer, Object> saveConsumer;

    /**
     * @param name         The database name of the statistic.
     * @param sorter       The way the statistic should be sorted from top to bottom.
     * @param ignored      Whether it should be ignored in the list of end game statistic messages.
     * @param coins        The coins awarded each time you increment the statistic.
     * @param slot         The slot in the statistic menu it is placed.
     * @param saveConsumer The way that the statistic is saved into the database.
     */
    public StatType(String name, SessionStatRegistry.Sorter sorter, boolean ignored, int coins, int slot, BiConsumer<NetworkPlayer, Object> saveConsumer) {
        this.name = name;
        this.sorter = sorter;
        this.coins = coins;
        this.slot = slot;
        this.ignored = ignored;
        this.saveConsumer = saveConsumer;

        this.messageTop = (sessionStatData, player) -> new ComplexBabelMessage()
                .append(Babel.translate(name.replaceAll("\\.", "_") + "_top"), NetworkPlayer.of(player).getName(), sessionStatData.get());
        this.messagePersonal = (sessionStatData, player) -> new ComplexBabelMessage()
                .append(Babel.translate(name.replaceAll("\\.", "_") + "_personal"), sessionStatData.get());
    }

    public StatType(String name, boolean ignored, int coins, int slot, GameRegistry.Type game) {
        this(name, SessionStatRegistry.Sorter.HIGHEST_SCORE, ignored, coins, slot, (p, o) -> p.getPersistentStats().getIntegerStatistics(game).increment(name, (int) o));
    }

    public StatType(String name, SessionStatRegistry.Sorter sorter, boolean ignored, int coins, int slot, GameRegistry.Type game) {
        this(name, sorter, ignored, coins, slot, (p, o) -> p.getPersistentStats().getIntegerStatistics(game).increment(name, (int) o));
    }

    public StatType(String name, SessionStatRegistry.Sorter sorter, BiConsumer<NetworkPlayer, Object> saveConsumer, int slot) {
        this(name, sorter, true, 0, slot, saveConsumer);
    }

}

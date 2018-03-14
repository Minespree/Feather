package net.minespree.feather.settings;

import lombok.AllArgsConstructor;
import lombok.Getter;
import net.minespree.feather.player.NetworkPlayer;
import net.minespree.feather.player.rank.Rank;
import net.minespree.feather.repository.Element;
import net.minespree.feather.repository.types.Type;

@Getter
@AllArgsConstructor
public class Setting implements Element {
    protected final String id;
    protected final String babelName;
    protected final String babelDescription;
    protected final Rank requiredRank;

    protected final Type type;
    protected final Object defaultValue;

    protected final long interval;

    public Setting(String id, String babelName, String babelDescription, Type type, Object defaultValue, long interval) {
        this(id, babelName, babelDescription, Rank.MEMBER, type, defaultValue, interval);
    }

    public Setting(String id, String babelName, String babelDescription, Rank requiredRank, Type type, Object defaultValue) {
        this(id, babelName, babelDescription, requiredRank, type, defaultValue, 2000L);
    }

    public Setting(String id, String babelName, String babelDescription, Type type, Object defaultValue) {
        this(id, babelName, babelDescription, type, defaultValue, 2000L);
    }

    public boolean canUse(NetworkPlayer player) {
        return player.getRank().has(requiredRank);
    }

    public boolean hasDescription() {
        return babelDescription != null;
    }

    public boolean hasDefaultValue() {
        return defaultValue != null;
    }
}

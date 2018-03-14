package net.minespree.feather.achievements;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.minespree.feather.repository.types.Type;

import java.util.Set;

@Getter
@RequiredArgsConstructor
public class SimpleAchievement implements Achievement {
    protected final String id;
    protected final String babelName;
    protected final String babelDescription;

    protected final Set<AchievementReward> rewards;

    protected final Type type;
    protected final Object defaultValue;

    @Override
    public String getBabel() {
        return babelName;
    }

    @Override
    public boolean hasDescription() {
        return babelDescription != null;
    }

    @Override
    public boolean hasDefaultValue() {
        return defaultValue != null;
    }

    @Override
    public String toString() {
        return "SimpleAchievement{id='" + id + "',type=" + type.getName() + "'}";
    }
}

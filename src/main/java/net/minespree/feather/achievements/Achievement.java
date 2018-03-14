package net.minespree.feather.achievements;

import net.minespree.feather.repository.Element;

import java.util.Set;

public interface Achievement extends Element {
    String getBabel();

    Set<AchievementReward> getRewards();

    String getBabelDescription();

    boolean hasDescription();
}

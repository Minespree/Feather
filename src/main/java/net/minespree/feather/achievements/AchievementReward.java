package net.minespree.feather.achievements;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class AchievementReward {
    private RewardType type;
    private int value;
}

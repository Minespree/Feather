package net.minespree.feather.player.achievements.callbacks;

import net.minespree.feather.achievements.Achievement;
import net.minespree.feather.achievements.AchievementReward;
import net.minespree.feather.achievements.RewardType;
import net.minespree.feather.player.NetworkPlayer;
import net.minespree.feather.player.achievements.PlayerAchievementCallback;
import net.minespree.feather.repository.types.EnumType;

import java.util.Set;

public class AchievementRewardCallback extends PlayerAchievementCallback {
    @Override
    public void notifyChange(NetworkPlayer player, Achievement achievement, Object oldValue, Object newValue) {
        // Enum types don't have a direct reward
        // TODO Add tiered rewards
        if (achievement.getType() instanceof EnumType) {
            return;
        }

        Set<AchievementReward> rewards = achievement.getRewards();

        for (AchievementReward reward : rewards) {
            RewardType type = reward.getType();

            type.apply(player, reward.getValue());
            type.sendRewardMessage(player, reward.getValue());
        }
    }
}

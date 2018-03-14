package net.minespree.feather.player.achievements;

import net.minespree.feather.achievements.Achievement;
import net.minespree.feather.player.NetworkPlayer;
import net.minespree.feather.repository.RepoCallbackManager;
import net.minespree.feather.repository.player.PlayerRepoManager;

public class PlayerAchievementManager extends PlayerRepoManager<Achievement> {
    public PlayerAchievementManager(NetworkPlayer player, RepoCallbackManager<Achievement> callbackManager) {
        super(player, callbackManager);
    }
}

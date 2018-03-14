package net.minespree.feather.player.achievements;

import com.google.common.base.Preconditions;
import net.minespree.feather.achievements.Achievement;
import net.minespree.feather.achievements.impl.FeatherAchievements;
import net.minespree.feather.player.NetworkPlayer;
import net.minespree.feather.player.achievements.callbacks.AchievementEndMessageCallback;
import net.minespree.feather.player.achievements.callbacks.AchievementMessageCallback;
import net.minespree.feather.player.achievements.callbacks.AchievementRewardCallback;
import net.minespree.feather.repository.RepoCallbackManager;
import net.minespree.feather.repository.RepoRegistry;
import net.minespree.feather.repository.base.SimpleRepoCallbackManager;
import net.minespree.feather.repository.base.SimpleRepoRegistry;

public class PlayerAchievements {
    public static final String MONGO_DOC = "achievements";

    private static final RepoRegistry<Achievement> registry = new SimpleRepoRegistry<>();
    private static final RepoCallbackManager<Achievement> callbackManager = new SimpleRepoCallbackManager<>();

    public static void init() {
        callbackManager.addGlobalCallback(new AchievementMessageCallback());
        callbackManager.addGlobalCallback(new AchievementRewardCallback());
        callbackManager.addGlobalCallback(new AchievementEndMessageCallback());

        FeatherAchievements.register(registry);
    }

    public static RepoRegistry<Achievement> getRegistry() {
        return registry;
    }

    public static RepoCallbackManager<Achievement> getCallbackManager() {
        return callbackManager;
    }

    public static PlayerAchievementManager createManager(NetworkPlayer player) {
        Preconditions.checkNotNull(player);

        return new PlayerAchievementManager(player, callbackManager);
    }
}

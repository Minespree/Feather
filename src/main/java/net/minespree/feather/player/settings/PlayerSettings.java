package net.minespree.feather.player.settings;

import com.google.common.base.Preconditions;
import net.minespree.feather.player.NetworkPlayer;
import net.minespree.feather.repository.RepoCallbackManager;
import net.minespree.feather.repository.RepoRegistry;
import net.minespree.feather.repository.base.SimpleRepoCallbackManager;
import net.minespree.feather.repository.base.SimpleRepoRegistry;
import net.minespree.feather.settings.FeatherSettings;
import net.minespree.feather.settings.Setting;

public class PlayerSettings {
    public static final String MONGO_DOC = "settings";

    private static final RepoRegistry<Setting> registry = new SimpleRepoRegistry<>();
    private static final RepoCallbackManager<Setting> callbackManager = new SimpleRepoCallbackManager<>();

    public static void init() {
        FeatherSettings.register(registry);
    }

    public static RepoRegistry<Setting> getRegistry() {
        return registry;
    }

    public static RepoCallbackManager<Setting> getCallbackManager() {
        return callbackManager;
    }

    public static PlayerSettingManager createManager(NetworkPlayer player) {
        Preconditions.checkNotNull(player);

        return new PlayerSettingManager(player, callbackManager);
    }


}

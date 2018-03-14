package net.minespree.feather.player.settings;

import net.minespree.feather.player.NetworkPlayer;
import net.minespree.feather.repository.RepoCallbackManager;
import net.minespree.feather.repository.player.PlayerRepoManager;
import net.minespree.feather.settings.Setting;

public class PlayerSettingManager extends PlayerRepoManager<Setting> {
    public PlayerSettingManager(NetworkPlayer player, RepoCallbackManager<Setting> callbackManager) {
        super(player, callbackManager);
    }
}

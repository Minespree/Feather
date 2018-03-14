package net.minespree.feather.player.perks;

import com.google.common.base.Preconditions;
import net.minespree.feather.data.gamedata.perks.Perk;
import net.minespree.feather.player.NetworkPlayer;
import net.minespree.feather.repository.RepoCallbackManager;
import net.minespree.feather.repository.RepoRegistry;
import net.minespree.feather.repository.base.SimpleRepoCallbackManager;
import net.minespree.feather.repository.base.SimpleRepoRegistry;

public class PlayerPerks {

    public static final String MONGO_DOC = "perks";

    private static final RepoRegistry<Perk> registry = new SimpleRepoRegistry<>();
    private static final RepoCallbackManager<Perk> callbackManager = new SimpleRepoCallbackManager<>();

    public static void init() {
        callbackManager.addGlobalCallback(new PlayerPerkCallback());
    }

    public static RepoRegistry<Perk> getRegistry() {
        return registry;
    }

    public static RepoCallbackManager<Perk> getCallbackManager() {
        return callbackManager;
    }

    public static PlayerPerkManager createManager(NetworkPlayer player) {
        Preconditions.checkNotNull(player);

        return new PlayerPerkManager(player, callbackManager);
    }

}

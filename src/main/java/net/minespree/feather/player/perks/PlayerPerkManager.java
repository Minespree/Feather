package net.minespree.feather.player.perks;

import com.google.common.base.Preconditions;
import com.google.common.collect.Maps;
import lombok.Getter;
import net.minespree.feather.data.gamedata.GameRegistry;
import net.minespree.feather.data.gamedata.perks.Perk;
import net.minespree.feather.data.gamedata.perks.PerkHandler;
import net.minespree.feather.player.NetworkPlayer;
import net.minespree.feather.player.PlayerKey;
import net.minespree.feather.repository.RepoCallbackManager;
import net.minespree.feather.repository.player.PlayerRepoManager;
import org.bson.Document;

import java.util.Map;

@Getter
public class PlayerPerkManager extends PlayerRepoManager<Perk> {

    private Map<GameRegistry.Type, PlayerPerkSet> perkSets = Maps.newHashMap();
    private Map<GameRegistry.Type, Integer> defaultSets = Maps.newHashMap();

    public PlayerPerkManager(NetworkPlayer player, RepoCallbackManager<Perk> callbackManager) {
        super(player, callbackManager);
    }

    public void loadSets(Document document) {
        if (document.containsKey(PlayerKey.SETS)) {
            Document sets = (Document) document.get(PlayerKey.SETS);
            for (String gameStr : sets.keySet()) {
                Document gameDoc = (Document) sets.get(gameStr);
                GameRegistry.Type game = GameRegistry.Type.byId(gameStr);

                if (gameDoc.containsKey("defaultSet")) {
                    defaultSets.put(game, gameDoc.getInteger("defaultSet"));
                }

                PlayerPerkSet set = new PlayerPerkSet(game, player);
                set.load(gameDoc);

                perkSets.put(game, set);
            }
        }
        for (GameRegistry.Type type : GameRegistry.Type.values()) {
            if (PerkHandler.getInstance().hasPerks(type) && !perkSets.containsKey(type)) {
                perkSets.put(type, new PlayerPerkSet(type, player));
            }
        }
    }

    public boolean hasDefault(GameRegistry.Type game) {
        return defaultSets.containsKey(game);
    }

    public int getDefaultSet(GameRegistry.Type game) {
        return defaultSets.getOrDefault(game, -1);
    }

    public PlayerPerkSet getPerkSet(GameRegistry.Type game) {
        Preconditions.checkNotNull(game, "Game is null");
        Preconditions.checkArgument(perkSets.containsKey(game), "There is no perk set for " + game);
        return perkSets.get(game);
    }

}
package net.minespree.feather.player.perks;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import net.minespree.feather.data.gamedata.GameRegistry;
import net.minespree.feather.data.gamedata.perks.Perk;
import net.minespree.feather.data.gamedata.perks.PerkHandler;
import net.minespree.feather.data.gamedata.perks.PerkSet;
import net.minespree.feather.player.NetworkPlayer;
import net.minespree.feather.player.PlayerKey;
import org.bson.Document;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

public class PlayerPerkSet {

    private Map<Integer, PerkSet> sets = Maps.newHashMap();

    private GameRegistry.Type game;
    private NetworkPlayer player;

    public PlayerPerkSet(GameRegistry.Type game, NetworkPlayer player) {
        this.game = game;
        this.player = player;
    }

    public void load(Document document) {
        for (String s : document.keySet()) {
            if (s.equals("defaultSet")) {
                continue;
            }
            Integer position = Integer.parseInt(s);

            List<String> perkIds = (List<String>) document.get(s);
            if (!perkIds.isEmpty()) {
                List<Perk> perks = perkIds.stream().filter(Objects::nonNull).map(perk -> PerkHandler.getInstance().getPerk(perk)).collect(Collectors.toCollection(LinkedList::new));

                sets.put(position, new PerkSet(player, perks, game, position));
            }
        }
    }

    public PerkSet createSet(int position) {
        PerkSet set = new PerkSet(player, Lists.newLinkedList(), game, position);

        player.addUpdate(PlayerKey.SETS + "." + game.name() + "." + position, Collections.emptyList());

        sets.put(position, set);
        return set;
    }

    public PerkSet getPerkSet(int position) {
        return sets.getOrDefault(position, null);
    }

}

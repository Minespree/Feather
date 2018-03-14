package net.minespree.feather.player.perks;

import net.minespree.feather.data.gamedata.perks.Perk;
import net.minespree.feather.player.NetworkPlayer;
import net.minespree.feather.player.PlayerKey;
import net.minespree.feather.repository.player.PlayerRepoCallback;

public class PlayerPerkCallback extends PlayerRepoCallback<Perk> {
    @Override
    public void notifyChange(NetworkPlayer player, Perk perk, Object oldValue, Object newValue) {
        player.addUpdate(PlayerKey.PERKS + "." + perk.getId(), "" + newValue);
    }
}

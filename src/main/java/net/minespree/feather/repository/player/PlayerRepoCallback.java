package net.minespree.feather.repository.player;

import net.minespree.feather.player.NetworkPlayer;
import net.minespree.feather.repository.Element;
import net.minespree.feather.repository.RepoCallback;
import net.minespree.feather.repository.RepoManager;
import net.minespree.feather.repository.base.SimpleRepoCallbackManager;

public abstract class PlayerRepoCallback<T extends Element> implements RepoCallback<T> {
    public abstract void notifyChange(NetworkPlayer player, T element, Object oldValue, Object newValue);

    @Override
    public void notifyChange(RepoManager manager, T element, Object oldValue, Object newValue) {
        if (manager instanceof PlayerRepoManager) {
            NetworkPlayer player = ((PlayerRepoManager) manager).getPlayer();

            this.notifyChange(player, element, oldValue, newValue);
        }
    }

    protected void yield() {
        SimpleRepoCallbackManager.yield();
    }
}

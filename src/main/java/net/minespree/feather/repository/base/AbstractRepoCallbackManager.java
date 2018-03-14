package net.minespree.feather.repository.base;

import net.minespree.feather.repository.Element;
import net.minespree.feather.repository.RepoCallback;
import net.minespree.feather.repository.RepoCallbackManager;

import java.util.List;

public abstract class AbstractRepoCallbackManager<T extends Element> implements RepoCallbackManager<T> {
    @Override
    public List<RepoCallback<T>> getCallbacks(T element) {
        return getCallbacks(element, false);
    }

    @Override
    public int getNumCallbacks(T element) {
        return getNumCallbacks(element, false);
    }

    @Override
    public boolean hasCallbacks(T element) {
        return hasCallbacks(element, false);
    }
}

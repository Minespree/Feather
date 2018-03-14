package net.minespree.feather.repository;

import java.util.List;

public interface RepoCallbackManager<T extends Element> {
    List<RepoCallback<T>> getCallbacks(T element);

    List<RepoCallback<T>> getCallbacks(T element, boolean includeGlobal);

    int getNumCallbacks(T element);

    int getNumCallbacks(T element, boolean includeGlobal);

    boolean hasCallbacks(T element);

    boolean hasCallbacks(T element, boolean includeGlobal);

    boolean addCallback(T element, RepoCallback<T> callback);

    int clearCallbacks(T element);

    boolean removeCallback(T element, RepoCallback<T> callback);

    List<RepoCallback<T>> getGlobalCallbacks();

    boolean addGlobalCallback(RepoCallback<T> callback);

    boolean removeGlobalCallback(RepoCallback<T> callback);

    void notifyChange(RepoManager<T> manager, T element, Object oldValue, Object newValue, Object rawValue, boolean notifyGlobal, Runnable changeCallback);
}

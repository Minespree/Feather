package net.minespree.feather.repository;

public interface RepoCallback<T extends Element> {
    void notifyChange(RepoManager manager, T element, Object oldValue, Object newValue);
}

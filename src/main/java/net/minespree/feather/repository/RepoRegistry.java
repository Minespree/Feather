package net.minespree.feather.repository;

import java.util.Collection;

public interface RepoRegistry<T extends Element> {
    T get(String id);

    T find(String id) throws IllegalArgumentException;

    Collection<T> getElements();

    boolean isRegistered(T element);

    void register(T element);

    void registerAll(Collection<T> elements);

    boolean unregister(T element);
}

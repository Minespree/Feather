package net.minespree.feather.repository.base;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import net.minespree.feather.repository.Element;
import net.minespree.feather.repository.RepoRegistry;

import java.util.Collection;
import java.util.Set;

public class SimpleRepoRegistry<T extends Element> implements RepoRegistry<T> {
    protected final Set<T> elements = Sets.newLinkedHashSet();

    @Override
    public T get(String id) {
        Preconditions.checkNotNull(id);

        for (T element : elements) {
            if (element.getId().equalsIgnoreCase(id)) {
                return element;
            }
        }

        return null;
    }

    @Override
    public T find(String id) {
        T element = get(id);

        if (element != null) {
            return element;
        }

        throw new IllegalArgumentException("Failed to find element for '" + id + "'");
    }

    @Override
    public Collection<T> getElements() {
        return ImmutableSet.copyOf(elements);
    }

    @Override
    public boolean isRegistered(T element) {
        Preconditions.checkNotNull(element);

        return elements.contains(element);
    }

    @Override
    public void register(T element) {
        Preconditions.checkNotNull(element);
        Preconditions.checkArgument(get(element.getId()) == null, "element already registered to id '%s'", element.getId());

        elements.add(element);
    }

    @Override
    public void registerAll(Collection<T> elements) {
        elements.forEach(this::register);
    }

    @Override
    public boolean unregister(T element) {
        return elements.remove(element);
    }
}

package net.minespree.feather.repository.base;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterators;
import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Multimaps;
import com.google.common.collect.SetMultimap;
import net.minespree.feather.repository.Element;
import net.minespree.feather.repository.RepoCallback;
import net.minespree.feather.repository.RepoManager;

import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

public class SimpleRepoCallbackManager<T extends Element> extends AbstractRepoCallbackManager<T> {
    private static final ThreadLocal<Runnable> YIELDER = new ThreadLocal<>();
    protected final SetMultimap<T, RepoCallback<T>> callbacks = Multimaps.synchronizedSetMultimap(LinkedHashMultimap.create());
    protected final Set<RepoCallback<T>> globalCallbacks = new CopyOnWriteArraySet<>();

    public static void yield() {
        Preconditions.checkState(YIELDER.get() != null, "cannot call yield() outside of a change operation");
        YIELDER.get().run();
    }

    @Override
    public List<RepoCallback<T>> getCallbacks(T element, boolean includeGlobal) {
        Preconditions.checkNotNull(element);

        Set<RepoCallback<T>> callbacks = this.callbacks.get(element);

        if (includeGlobal) {
            return new ImmutableList.Builder<RepoCallback<T>>().addAll(callbacks).addAll(globalCallbacks).build();
        } else {
            return ImmutableList.copyOf(callbacks);
        }
    }

    @Override
    public int getNumCallbacks(T element, boolean includeGlobal) {
        Preconditions.checkNotNull(element);

        int localCount = callbacks.get(element).size();
        int globalCount = includeGlobal ? globalCallbacks.size() : 0;

        return localCount + globalCount;
    }

    @Override
    public boolean hasCallbacks(T element, boolean includeGlobal) {
        Preconditions.checkNotNull(element);

        boolean hasLocal = callbacks.containsKey(element);
        boolean hasGlobal = includeGlobal && !globalCallbacks.isEmpty();

        return hasLocal || hasGlobal;
    }

    @Override
    public boolean addCallback(T element, RepoCallback<T> callback) {
        Preconditions.checkNotNull(element);
        Preconditions.checkNotNull(callback);

        return callbacks.put(element, callback);
    }

    @Override
    public int clearCallbacks(T element) {
        Preconditions.checkNotNull(element);

        return callbacks.removeAll(element).size();
    }

    @Override
    public boolean removeCallback(T element, RepoCallback<T> callback) {
        Preconditions.checkNotNull(element);
        Preconditions.checkNotNull(callback);

        return callbacks.remove(element, callback);
    }

    @Override
    public List<RepoCallback<T>> getGlobalCallbacks() {
        return ImmutableList.copyOf(globalCallbacks);
    }

    @Override
    public boolean addGlobalCallback(RepoCallback<T> callback) {
        Preconditions.checkNotNull(callback);

        return globalCallbacks.add(callback);
    }

    @Override
    public boolean removeGlobalCallback(RepoCallback<T> callback) {
        Preconditions.checkNotNull(callback);

        return globalCallbacks.remove(callback);
    }

    @Override
    public void notifyChange(RepoManager<T> manager, T element, Object oldValue, Object newValue, Object rawValue, boolean notifyGlobal, Runnable changeCallback) {
        Preconditions.checkNotNull(manager);
        Preconditions.checkNotNull(element);
        Preconditions.checkNotNull(oldValue);
        Preconditions.checkNotNull(newValue);

        boolean hasChanged = !Objects.equals(oldValue, newValue);

        if (notifyGlobal && hasChanged) {
            for (RepoCallback<T> callback : ImmutableSet.copyOf(globalCallbacks)) {
                callback.notifyChange(manager, element, oldValue, newValue);
            }
        }

        if (hasChanged) {
            dispatch(manager, element, oldValue, newValue,
                    Iterators.concat(
                            ImmutableSet.copyOf(callbacks.get(element)).iterator(),
                            Iterators.singletonIterator((manager1, element1, oldValue1, newValue1) -> changeCallback.run())
                    )
            );
        }
    }

    private void dispatch(RepoManager<T> manager, T element, Object oldValue, Object newValue, Iterator<RepoCallback<T>> callbacks) {
        if (!callbacks.hasNext()) return;

        final Runnable old = YIELDER.get();

        YIELDER.set(new Runnable() {
            boolean yielded;

            @Override
            public void run() {
                if (yielded) return;

                yielded = true;
                dispatch(manager, element, oldValue, newValue, callbacks);
            }
        });

        try {
            callbacks.next().notifyChange(manager, element, oldValue, newValue);
            YIELDER.get().run(); // If callback didn't yield, do it ourselves
        } finally {
            YIELDER.set(old);
        }
    }
}

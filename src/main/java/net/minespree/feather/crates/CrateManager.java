package net.minespree.feather.crates;

import java.util.Collection;
import java.util.stream.Collectors;

public interface CrateManager {
    Collection<Crate> getCrates();

    default int getCount() {
        return getCrates().size();
    }

    default Collection<Crate> getCrates(CrateType type) {
        return getCrates().stream().filter(crate -> crate.getType().equals(type)).collect(Collectors.toSet());
    }

    default Collection<Crate> getSuperCrates() {
        return getCrates().stream().filter(Crate::isSuperCrate).collect(Collectors.toSet());
    }

    default int getTypeCount(CrateType type) {
        return getCrates(type).size();
    }

    void addCrate(Crate crate);

    /**
     * @see #removeCrate(Crate) where forceDelete argument is {@code false}
     */
    default boolean removeCrate(Crate crate) {
        return removeCrate(crate, false);
    }

    /**
     * Removes a present crate stored in {@link #getCrates()}. If {@code forceDelete} is
     * {@code false} and the crate is seasonal and expires, it will autoconvert it
     * to a {@link CrateType#DEFAULT} crate.
     *
     * @return whether the crate was removed successfully from Mongo
     */
    boolean removeCrate(Crate crate, boolean forceDelete);

    void cleanupExpired();

    default int getSuperAmount() {
        return (int) getCrates().stream().filter(Crate::isSuperCrate).count();
    }

    default int getRegularAmount() {
        return getCount() - getSuperAmount();
    }
}

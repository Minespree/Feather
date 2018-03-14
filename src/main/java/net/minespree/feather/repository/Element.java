package net.minespree.feather.repository;

import net.minespree.feather.repository.types.Type;

public interface Element {
    String getId();

    Type getType();

    Object getDefaultValue();

    boolean hasDefaultValue();
}

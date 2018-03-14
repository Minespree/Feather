package net.minespree.feather.repository.types.util;

public interface Toggleable {
    Object getNextState(Object previous) throws IllegalArgumentException;
}

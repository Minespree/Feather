package net.minespree.feather.repository.types;

public interface Type {
    String getName();

    boolean isInstance(Object obj);

    Object getDefault();

    // TODO Change by Mongo object
    Object parse(String raw) throws TypeParseException;

    String toString(Object obj) throws IllegalArgumentException;
}

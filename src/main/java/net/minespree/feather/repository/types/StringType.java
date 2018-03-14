package net.minespree.feather.repository.types;

public class StringType implements Type {
    @Override
    public String getName() {
        return "String";
    }

    @Override
    public boolean isInstance(Object obj) {
        return obj instanceof String;
    }

    @Override
    public Object getDefault() {
        return null;
    }

    @Override
    public Object parse(String raw) {
        return raw;
    }

    @Override
    public String toString(Object obj) throws IllegalArgumentException {
        return String.valueOf(obj);
    }
}

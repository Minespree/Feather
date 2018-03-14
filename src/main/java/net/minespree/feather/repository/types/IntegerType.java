package net.minespree.feather.repository.types;

import com.google.common.base.Preconditions;

public class IntegerType implements Type {
    @Override
    public String getName() {
        return "Integer";
    }

    @Override
    public boolean isInstance(Object obj) {
        return obj instanceof Integer;
    }

    @Override
    public Object getDefault() {
        return null;
    }

    @Override
    public Object parse(String raw) throws TypeParseException {
        try {
            return Integer.parseInt(raw);
        } catch (NumberFormatException e) {
            throw new TypeParseException();
        }
    }

    @Override
    public String toString(Object obj) throws IllegalArgumentException {
        Preconditions.checkArgument(isInstance(obj));

        return Integer.toString((int) obj);
    }
}

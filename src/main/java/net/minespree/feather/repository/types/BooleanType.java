package net.minespree.feather.repository.types;

import com.google.common.base.Preconditions;
import net.minespree.feather.repository.types.util.Toggleable;
import net.minespree.feather.repository.types.util.TypeUtil;

public class BooleanType implements Type, Toggleable {
    @Override
    public String getName() {
        return "Boolean";
    }

    @Override
    public boolean isInstance(Object obj) {
        return obj instanceof Boolean;
    }

    @Override
    public Object getDefault() {
        return false;
    }

    @Override
    public Object parse(String raw) throws TypeParseException {
        raw = raw.toLowerCase().trim();

        if (raw.equals("on") || raw.equals("true") || raw.equals("yes")) {
            return true;
        } else if (raw.equals("off") || raw.equals("false") || raw.equals("no")) {
            return false;
        }

        throw new TypeParseException();
    }

    @Override
    public String toString(Object obj) throws IllegalArgumentException {
        Preconditions.checkArgument(isInstance(obj));

        return Boolean.toString((boolean) obj);
    }

    @Override
    public Object getNextState(Object previous) throws IllegalArgumentException {
        Boolean value = TypeUtil.getValue(previous, Boolean.class);

        // Wrap null values
        return Boolean.valueOf(!value);
    }
}

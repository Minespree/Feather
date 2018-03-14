package net.minespree.feather.repository.types.util;

import com.google.common.base.Preconditions;

public class TypeUtil {
    private TypeUtil() {
    }

    public static <T> T getValue(Object value, Class<T> typeClass) throws IllegalArgumentException {
        Preconditions.checkNotNull(typeClass);

        if (value != null) {
            Preconditions.checkArgument(typeClass.isAssignableFrom(value.getClass()), "value can't be casted to %s", typeClass.getName());
            return (T) value;
        }

        return null;
    }
}

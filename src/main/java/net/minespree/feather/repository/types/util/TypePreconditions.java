package net.minespree.feather.repository.types.util;

import com.google.common.base.Preconditions;
import net.minespree.feather.repository.types.Type;

public class TypePreconditions {
    public static void checkInstance(Type type, Object obj) throws IllegalArgumentException {
        Preconditions.checkNotNull(obj);
        Preconditions.checkArgument(type.isInstance(obj), "object is not an instance of " + type.getName());
    }
}

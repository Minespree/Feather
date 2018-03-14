package net.minespree.feather.util;

import java.lang.reflect.Array;

@SuppressWarnings("all")
public final class ArrayUtil {
    public static <T> T[] concat(Class<T[]> cls, T[]... ts) {
        if (ts.length == 0) {
            return (T[]) Array.newInstance(cls.getComponentType(), 0);
        }
        if (ts.length == 1) {
            return ts[0];
        }
        int len = 0;
        for (T[] t : ts) {
            len += t.length;
        }
        T[] t = (T[]) Array.newInstance(cls.getComponentType(), len);
        int offset = 0;
        for (T[] s : ts) {
            System.arraycopy(s, 0, t, offset, s.length);
            offset += s.length;
        }
        return t;
    }
}

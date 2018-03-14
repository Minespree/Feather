package net.minespree.feather.util.reflection;

import com.google.common.base.Preconditions;
import com.google.common.collect.Maps;
import org.bukkit.Bukkit;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Map;

public class ReflectionUtil {
    private static final String NMS_ROOT;
    private static final String CB_ROOT;
    private static final Cache CACHE = new Cache();

    static {
        CB_ROOT = Bukkit.getServer().getClass().getPackage().getName() + ".";
        NMS_ROOT = CB_ROOT.replace("org.bukkit.craftbukkit.", "net.minecraft.server.");
    }

    public static EasyClass getNMSClass(String name) {
        return getEasyClass(NMS_ROOT + name);
    }

    public static EasyClass getCBClass(String name) {
        return getEasyClass(CB_ROOT + name);
    }

    public static EasyClass getEasyClass(String name) {
        return getEasyClass(getRawClass(name));
    }

    public static EasyClass getEasyClass(Class<?> cls) {
        return CACHE.get(cls);
    }

    public static EasyField getNMSField(String className, String fieldName) {
        return getField(getRawClass(NMS_ROOT + className), fieldName);
    }

    public static EasyField getCBField(String className, String fieldName) {
        return getField(getRawClass(CB_ROOT + className), fieldName);
    }

    public static EasyField getField(Class<?> clazz, String name) {
        try {
            Field field = clazz.getDeclaredField(name);
            field.setAccessible(true);
            return new EasyField(field);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static EasyMethod getNMSMethod(String className, String methodName, Class<?>... params) {
        return getMethod(getRawClass(NMS_ROOT + className), methodName, params);
    }

    public static EasyMethod getCBMethod(String className, String methodName, Class<?>... params) {
        return getMethod(getRawClass(CB_ROOT + className), methodName, params);
    }

    public static EasyMethod getMethod(Class<?> clazz, String name, Class<?>... params) {
        try {
            Method method = clazz.getDeclaredMethod(name, params);
            method.setAccessible(true);
            return new EasyMethod(method);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private static Class<?> getRawClass(String name) {
        try {
            return Class.forName(name);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
            return null;
        }
    }

    private static class Cache {
        private final Map<Class<?>, EasyClass> byClass = Maps.newConcurrentMap();

        public EasyClass get(Class<?> cls) {
            Preconditions.checkNotNull(cls, "Class was not found");
            EasyClass safe = byClass.get(cls);
            if (safe == null) {
                safe = new EasyClass(cls);
                byClass.put(cls, safe);
            }
            return safe;
        }
    }
}

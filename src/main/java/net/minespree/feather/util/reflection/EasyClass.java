package net.minespree.feather.util.reflection;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;

public class EasyClass {
    private final Class<?> handle;
    private final Map<String, EasyField> fieldMap = Maps.newHashMap();
    private final Multimap<String, EasyMethod> methodMap = HashMultimap.create();

    @Deprecated
    public EasyClass(Class<?> instance) {
        this.handle = instance;
        scanFields(handle);
        scanMethods(handle);
    }

    private void scanFields(Class<?> clazz) {
        if (clazz == null) {
            return;
        }

        for (Field field : clazz.getDeclaredFields()) {
            if (!fieldMap.containsKey(field.getName())) {
                field.setAccessible(true);
                EasyField EasyField = new EasyField(field);
                fieldMap.put(field.getName(), EasyField);
            }
        }

        scanFields(clazz.getSuperclass());
    }

    private void scanMethods(Class<?> clazz) {
        if (clazz == null) {
            return;
        }

        for (Method method : clazz.getDeclaredMethods()) {
            method.setAccessible(true);
            EasyMethod EasyMethod = new EasyMethod(method);
            methodMap.put(method.getName(), EasyMethod);
        }

        scanMethods(clazz.getSuperclass());
    }

    public String getName() {
        return handle.getName();
    }

    public String getSimpleName() {
        return handle.getSimpleName();
    }

    public EasyField getField(String name) {
        return fieldMap.get(name);
    }

    public EasyMethod getMethod(String name) {
        return methodMap.containsKey(name) ? methodMap.get(name).stream().findFirst().orElse(null) : null;
    }

    public EasyMethod getMethod(String name, Class<?>... params) {
        return methodMap.containsKey(name)
                ? methodMap.get(name).stream().filter(easyMethod -> Arrays.equals(params, easyMethod.getHandle().getParameterTypes())).findFirst().orElse(null)
                : null;
    }

    public Collection<EasyField> getFields() {
        return Collections.unmodifiableCollection(fieldMap.values());
    }

    public Collection<EasyMethod> getMethods() {
        return Collections.unmodifiableCollection(methodMap.values());
    }

    public Class<?> getHandle() {
        return handle;
    }

    public <T> T newInstance(Class<T> type) {
        return type.cast(newInstance());
    }

    public Object newInstance() {
        try {
            return handle.newInstance();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public <T> T newInstance(Class<T> type, Class<?>[] types, Object... args) {
        return type.cast(newInstance(types, args));
    }

    public Object newInstance(Class<?>[] types, Object... args) {
        try {
            return handle.getDeclaredConstructor(types).newInstance(args);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public <T> T newInstance(Class<T> type, Object... args) {
        return type.cast(newInstance(args));
    }

    public Object newInstance(Object... args) {
        for (Constructor constructor : handle.getDeclaredConstructors()) {
            try {
                return constructor.newInstance(args);
            } catch (Exception ignored) {
            }
        }
        return null;
    }
}

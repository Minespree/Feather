package net.minespree.feather.util.reflection;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

public class EasyField {
    private final Field handle;

    protected EasyField(Field handle) {
        this.handle = handle;
    }

    public <T> T getStatic(Class<T> type) {
        return get(null, type);
    }

    public Object getStatic() {
        return get(null);
    }

    public void setStatic(Object value) {
        try {
            handle.set(null, value);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
    }

    public <T> T get(Object instance, Class<T> type) {
        return type.cast(get(instance));
    }

    public Object get(Object instance) {
        if (instance == null && Modifier.isFinal(handle.getModifiers())) {
            // If we don't unfinal it now, it will cause issues later when we set it.
            try {
                Field modifiers = Field.class.getDeclaredField("modifiers");
                modifiers.setAccessible(true);
                modifiers.set(handle, handle.getModifiers() & ~Modifier.FINAL);
            } catch (NoSuchFieldException | IllegalAccessException ex) {
                ex.printStackTrace();
            }
        }
        try {
            return handle.get(instance);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
            return null;
        }
    }

    public void set(Object instance, Object value) {
        try {
            handle.set(instance, value);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
    }

    public void setStaticFinal(Object value) {
        try {
            handle.set(null, value);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
    }

    public <T extends Annotation> T getAnnotation(Class<T> type) {
        return handle.getAnnotation(type);
    }

    public Field getHandle() {
        return handle;
    }
}

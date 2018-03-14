package net.minespree.feather.util.reflection;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;

@SuppressWarnings("unchecked")
public class EasyMethod {
    private Method handle;

    public EasyMethod(Method handle) {
        this.handle = handle;
    }

    public <T> T invokeStatic(Class<T> type, Object... args) {
        return type.cast(invokeStatic(args));
    }

    public Object invokeStatic(Object... args) {
        return invoke(null, args);
    }

    public Object invoke(Object instance, Object... args) {
        try {
            return handle.invoke(instance, args);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public <T extends Annotation> T getAnnotation(Class<T> type) {
        return handle.getAnnotation(type);
    }

    public Method getHandle() {
        return handle;
    }


}

package net.minespree.feather.repository.base;

import com.google.common.base.Preconditions;
import net.minespree.feather.repository.Element;
import net.minespree.feather.repository.RepoManager;
import net.minespree.feather.repository.types.util.TypeUtil;

public abstract class AbstractRepoManager<T extends Element> implements RepoManager<T> {
    public abstract Object getRawValue(T element);

    protected abstract void setRawValue(T element, Object value);

    @Override
    public boolean hasValue(T element) {
        Preconditions.checkNotNull(element);

        return getRawValue(element) != null;
    }

    @Override
    public Object getValue(T element) {
        return getValue(element, element.getDefaultValue());
    }

    @Override
    public Object getValue(T element, Object defaultValue) throws IllegalArgumentException {
        Preconditions.checkNotNull(element);
        Preconditions.checkNotNull(defaultValue);
        Preconditions.checkArgument(element.getType().isInstance(defaultValue), "default value must be instance of element type");

        Object value = getRawValue(element);

        if (value != null) {
            return value;
        }

        return defaultValue;
    }

    @Override
    public <V> V getRawValue(T element, Class<V> typeClass) throws IllegalArgumentException {
        Preconditions.checkNotNull(element);
        Preconditions.checkNotNull(typeClass);

        Object rawValue = getRawValue(element);

        if (rawValue != null) {
            return TypeUtil.getValue(rawValue, typeClass);
        }

        return null;
    }

    @Override
    public <V> V getValue(T element, Class<V> typeClass) throws IllegalArgumentException {
        return getValue(element, typeClass, (V) element.getDefaultValue());
    }

    @Override
    public <V> V getValue(T element, Class<V> typeClass, V defaultValue) throws IllegalArgumentException {
        Preconditions.checkNotNull(element);
        Preconditions.checkNotNull(typeClass);
        Preconditions.checkNotNull(defaultValue);

        V value = getRawValue(element, typeClass);

        if (value != null) {
            return value;
        }

        return null;
    }

    @Override
    public boolean setValue(T element, Object value) {
        return setValue(element, value, false);
    }

    @Override
    public boolean setValue(T element, Object value, boolean notifyGlobal) {
        Preconditions.checkNotNull(element);
        Preconditions.checkArgument(value == null || element.getType().isInstance(value), "value isn't the correct type");

        Object oldValue = getValue(element);
        Object newValue = value != null ? value : element.getDefaultValue();

        if (oldValue == newValue) {
            return false;
        }

        getCallbackManager().notifyChange(this, element, oldValue, newValue, value, notifyGlobal, () -> {
            setRawValue(element, value);
        });

        return true;
    }

    @Override
    public void deleteValue(T element) {
        setValue(element, null);
    }
}

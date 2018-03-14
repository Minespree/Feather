package net.minespree.feather.util;

import java.util.function.Consumer;

public class Reference<T> {
    private T value;

    public Reference() {
    }

    public Reference(T def) {
        this.value = def;
    }

    public T getValue() {
        return this.value;
    }

    public void setValue(T value) {
        this.value = value;
    }

    public boolean hasValue() {
        return this.value != null;
    }

    public void ifPresent(Consumer<T> consumer) {
        if (this.hasValue()) {
            consumer.accept(this.value);
        }
    }
}

package net.minespree.feather.util;

import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Supplier;

@SuppressWarnings("all")
public class OptionalConsumer<T> {
    private Optional<T> optional;

    private OptionalConsumer(Optional<T> optional) {
        this.optional = optional;
    }

    public static <T> OptionalConsumer<T> of(Optional<T> optional) {
        return new OptionalConsumer<>(optional);
    }

    public OptionalConsumer<T> ifPresent(Consumer<T> c) {
        optional.ifPresent(c);
        return this;
    }

    public OptionalConsumer<T> orElse(Runnable r) {
        if (!optional.isPresent())
            r.run();
        return this;
    }

    public OptionalConsumer<T> orElseThrow(Supplier<Exception> throwable) throws Exception {
        if (!optional.isPresent()) {
            throw throwable.get();
        }

        return this;
    }
}

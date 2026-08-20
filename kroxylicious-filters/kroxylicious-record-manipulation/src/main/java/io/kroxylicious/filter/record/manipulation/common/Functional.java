/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.record.manipulation.common;

import java.util.function.Function;
import java.util.function.Supplier;

public class Functional {

    private Functional() {
    }

    public static <R, T> Function<T, R> asFunction(Supplier<R> supplier) {
        return (T ignored) -> supplier.get();
    }

    public static <T> Supplier<T> asSupplier(Runnable r) {
        return asSupplier(r, null);
    }

    public static <T> Supplier<T> asSupplier(Runnable runnable, T result) {
        return () -> {
            runnable.run();
            return result;
        };
    }

    public static <R, T> Function<T, R> toFn(Runnable runnable, R result) {
        return (T ignored) -> {
            runnable.run();
            return result;
        };
    }
}

/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.record.manipulation.common;

import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Adapters between {@link Runnable}, {@link Supplier}, and {@link Function}.
 */
public class Functional {

    private Functional() {
    }

    /**
     * Adapts a {@link Supplier} to a {@link Function} that ignores its input.
     * @param supplier the supplier to delegate to
     * @return a function that ignores its input and returns {@code supplier.get()}
     * @param <R> the result type
     * @param <T> the ignored input type
     */
    public static <R, T> Function<T, R> asFunction(Supplier<R> supplier) {
        return (T ignored) -> supplier.get();
    }

    /**
     * Adapts a {@link Runnable} to a {@link Supplier} that runs it and returns {@code null}.
     * @param r the runnable to delegate to
     * @return a supplier that runs {@code r} and returns {@code null}
     * @param <T> the result type
     */
    public static <T> Supplier<T> asSupplier(Runnable r) {
        return asSupplier(r, null);
    }

    /**
     * Adapts a {@link Runnable} to a {@link Supplier} that runs it and returns a fixed result.
     * @param runnable the runnable to delegate to
     * @param result the value to return after running {@code runnable}
     * @return a supplier that runs {@code runnable} and returns {@code result}
     * @param <T> the result type
     */
    public static <T> Supplier<T> asSupplier(Runnable runnable, T result) {
        return () -> {
            runnable.run();
            return result;
        };
    }

    /**
     * Adapts a {@link Runnable} to a {@link Function} that ignores its input, runs the runnable,
     * and returns a fixed result.
     * @param runnable the runnable to delegate to
     * @param result the value to return after running {@code runnable}
     * @return a function that ignores its input, runs {@code runnable}, and returns {@code result}
     * @param <R> the result type
     * @param <T> the ignored input type
     */
    public static <R, T> Function<T, R> toFn(Runnable runnable, R result) {
        return (T ignored) -> {
            runnable.run();
            return result;
        };
    }
}

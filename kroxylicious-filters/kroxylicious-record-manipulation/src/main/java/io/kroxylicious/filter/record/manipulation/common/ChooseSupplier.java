/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.record.manipulation.common;

import java.util.Set;
import java.util.function.Function;

/**
 * A function that returns a value drawn at random from a fixed set.
 *
 * @param <T> the type of the values
 */
public class ChooseSupplier<T> implements Function<Context, T> {
    private final Object[] values;

    /**
     * Creates an instance.
     * @param from the set of values to choose from
     */
    public ChooseSupplier(Set<T> from) {
        values = from.toArray(Object[]::new);
    }

    @SuppressWarnings("unchecked")
    @Override
    public T apply(Context context) {
        return (T) values[context.random().nextInt(0, values.length)];
    }
}

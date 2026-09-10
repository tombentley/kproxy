/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.record.manipulation.ops.choose;

import java.util.Set;
import java.util.function.Function;

import io.kroxylicious.filter.record.manipulation.op.OpContext;

/**
 * A function that returns a value drawn at random from a fixed set.
 *
 * @param <T> the type of the values
 */
class ChooseSupplier<T> implements Function<OpContext, T> {
    private final Object[] values;

    /**
     * Creates an instance.
     * @param from the set of values to choose from
     */
    ChooseSupplier(Set<T> from) {
        values = from.toArray(Object[]::new);
    }

    @SuppressWarnings("unchecked")
    @Override
    public T apply(OpContext opContext) {
        return (T) values[opContext.random().nextInt(0, values.length)];
    }
}

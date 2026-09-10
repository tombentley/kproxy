/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.record.manipulation.ops.choose;

import java.util.Set;
import java.util.function.ToLongFunction;

import io.kroxylicious.filter.record.manipulation.op.OpContext;

/**
 * A function that returns a {@code long} drawn at random from a fixed set.
 */
class ChooseLongSupplier implements ToLongFunction<OpContext> {
    private final long[] values;

    /**
     * Creates an instance.
     * @param from the set of values to choose from
     */
    ChooseLongSupplier(Set<Long> from) {
        values = from.stream().mapToLong(i -> i).toArray();
    }

    @Override
    public long applyAsLong(OpContext opContext) {
        int index = opContext.random().nextInt(0, values.length);
        return values[index];
    }
}

/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.record.manipulation.common;

import java.util.Set;
import java.util.function.ToLongFunction;

/**
 * A function that returns a {@code long} drawn at random from a fixed set.
 */
public class ChooseLongSupplier implements ToLongFunction<Context> {
    private final long[] values;

    /**
     * Creates an instance.
     * @param from the set of values to choose from
     */
    public ChooseLongSupplier(Set<Long> from) {
        values = from.stream().mapToLong(i -> i).toArray();
    }

    @Override
    public long applyAsLong(Context context) {
        int index = context.random().nextInt(0, values.length);
        return values[index];
    }
}

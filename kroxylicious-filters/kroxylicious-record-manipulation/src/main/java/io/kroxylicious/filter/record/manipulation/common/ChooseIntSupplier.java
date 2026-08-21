/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.record.manipulation.common;

import java.util.Set;
import java.util.function.ToIntFunction;

/**
 * A function that returns an {@code int} drawn at random from a fixed set.
 */
public class ChooseIntSupplier implements ToIntFunction<Context> {
    private final int[] values;

    /**
     * Creates an instance.
     * @param from the set of values to choose from
     */
    public ChooseIntSupplier(Set<Integer> from) {
        values = from.stream().mapToInt(i -> i).toArray();
    }

    @Override
    public int applyAsInt(Context context) {
        int index = context.random().nextInt(0, values.length);
        return values[index];
    }
}

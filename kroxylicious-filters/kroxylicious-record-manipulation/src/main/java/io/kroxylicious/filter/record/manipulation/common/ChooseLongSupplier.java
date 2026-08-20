/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.record.manipulation.common;

import java.util.Random;
import java.util.Set;
import java.util.function.LongSupplier;

/**
 * A supplier that returns a {@code long} drawn at random from a fixed set.
 */
public class ChooseLongSupplier implements LongSupplier {
    private final Random prng;
    private final long[] values;

    /**
     * Creates a supplier.
     * @param prng the source of randomness
     * @param from the set of values to choose from
     */
    public ChooseLongSupplier(Random prng, Set<Long> from) {
        this.prng = prng;
        values = from.stream().mapToLong(i -> i).toArray();
    }

    @Override
    public long getAsLong() {
        int index = prng.nextInt(0, values.length);
        return values[index];
    }
}

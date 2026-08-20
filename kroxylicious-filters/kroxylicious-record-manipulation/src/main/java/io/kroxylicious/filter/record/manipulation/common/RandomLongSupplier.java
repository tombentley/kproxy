/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.record.manipulation.common;

import java.util.Random;
import java.util.function.LongSupplier;

/**
 * A supplier that returns a {@code long} drawn at random from a range.
 */
public class RandomLongSupplier implements LongSupplier {
    private final Random prng;
    private final long minInclusive;
    private final long maxExclusive;

    /**
     * Creates a supplier.
     * @param prng the source of randomness
     * @param minInclusive the minimum value (inclusive)
     * @param maxExclusive the maximum value (exclusive)
     */
    public RandomLongSupplier(Random prng, long minInclusive, long maxExclusive) {
        if (minInclusive >= maxExclusive) {
            throw new IllegalArgumentException("minInclusive (" + minInclusive + ") must be < maxExclusive (" + maxExclusive + ")");
        }
        this.prng = prng;
        this.minInclusive = minInclusive;
        this.maxExclusive = maxExclusive;
    }

    @Override
    public long getAsLong() {
        return prng.nextLong(minInclusive, maxExclusive);
    }
}

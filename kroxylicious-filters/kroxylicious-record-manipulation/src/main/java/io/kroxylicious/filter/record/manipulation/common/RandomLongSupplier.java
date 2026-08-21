/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.record.manipulation.common;

import java.util.function.ToLongFunction;

/**
 * A function that returns a {@code long} drawn at random from a range.
 */
public class RandomLongSupplier implements ToLongFunction<Context> {
    private final long minInclusive;
    private final long maxExclusive;

    /**
     * Creates an instance.
     * @param minInclusive the minimum value (inclusive)
     * @param maxExclusive the maximum value (exclusive)
     */
    public RandomLongSupplier(long minInclusive, long maxExclusive) {
        if (minInclusive >= maxExclusive) {
            throw new IllegalArgumentException("minInclusive (" + minInclusive + ") must be < maxExclusive (" + maxExclusive + ")");
        }
        this.minInclusive = minInclusive;
        this.maxExclusive = maxExclusive;
    }

    @Override
    public long applyAsLong(Context context) {
        return context.random().nextLong(minInclusive, maxExclusive);
    }
}

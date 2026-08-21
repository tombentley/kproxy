/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.record.manipulation.common;

import java.util.function.ToIntFunction;

/**
 * A function that returns an {@code int} drawn at random from a range.
 */
public class RandomIntSupplier implements ToIntFunction<Context> {
    private final int minInclusive;
    private final int maxExclusive;

    /**
     * Creates an instance.
     * @param minInclusive the minimum value (inclusive)
     * @param maxExclusive the maximum value (exclusive)
     */
    public RandomIntSupplier(int minInclusive, int maxExclusive) {
        if (minInclusive >= maxExclusive) {
            throw new IllegalArgumentException("minInclusive (" + minInclusive + ") must be < maxExclusive (" + maxExclusive + ")");
        }
        this.minInclusive = minInclusive;
        this.maxExclusive = maxExclusive;
    }

    @Override
    public int applyAsInt(Context context) {
        return context.random().nextInt(minInclusive, maxExclusive);
    }
}

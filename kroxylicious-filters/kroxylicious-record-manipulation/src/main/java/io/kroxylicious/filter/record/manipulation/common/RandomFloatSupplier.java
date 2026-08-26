/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.record.manipulation.common;

import java.util.function.Function;

/**
 * A function that returns a {@code double} drawn at random from a range.
 */
public class RandomFloatSupplier implements Function<Context, Float> {
    private final float minInclusive;
    private final float maxExclusive;

    /**
     * Creates an instance.
     * @param minInclusive the minimum value (inclusive)
     * @param maxExclusive the maximum value (exclusive)
     */
    public RandomFloatSupplier(float minInclusive, float maxExclusive) {
        if (minInclusive >= maxExclusive) {
            throw new IllegalArgumentException("minInclusive (" + minInclusive + ") must be < maxExclusive (" + maxExclusive + ")");
        }
        this.minInclusive = minInclusive;
        this.maxExclusive = maxExclusive;
    }

    @Override
    public Float apply(Context context) {
        return context.random().nextFloat(minInclusive, maxExclusive);
    }
}

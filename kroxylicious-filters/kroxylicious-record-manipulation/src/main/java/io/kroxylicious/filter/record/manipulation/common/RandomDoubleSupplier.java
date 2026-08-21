/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.record.manipulation.common;

import java.util.function.ToDoubleFunction;

/**
 * A function that returns a {@code double} drawn at random from a range.
 */
public class RandomDoubleSupplier implements ToDoubleFunction<Context> {
    private final double minInclusive;
    private final double maxExclusive;

    /**
     * Creates an instance.
     * @param minInclusive the minimum value (inclusive)
     * @param maxExclusive the maximum value (exclusive)
     */
    public RandomDoubleSupplier(double minInclusive, double maxExclusive) {
        if (minInclusive >= maxExclusive) {
            throw new IllegalArgumentException("minInclusive (" + minInclusive + ") must be < maxExclusive (" + maxExclusive + ")");
        }
        this.minInclusive = minInclusive;
        this.maxExclusive = maxExclusive;
    }

    @Override
    public double applyAsDouble(Context context) {
        return context.random().nextDouble(minInclusive, maxExclusive);
    }
}

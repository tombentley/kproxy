/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.record.manipulation.ops.random;

import java.util.function.ToDoubleFunction;

import io.kroxylicious.filter.record.manipulation.op.OpContext;

/**
 * A function that returns a {@code double} drawn at random from a range.
 */
class RandomDoubleSupplier implements ToDoubleFunction<OpContext> {
    private final double minInclusive;
    private final double maxExclusive;

    /**
     * Creates an instance.
     * @param minInclusive the minimum value (inclusive)
     * @param maxExclusive the maximum value (exclusive)
     */
    RandomDoubleSupplier(double minInclusive, double maxExclusive) {
        if (minInclusive >= maxExclusive) {
            throw new IllegalArgumentException("minInclusive (" + minInclusive + ") must be < maxExclusive (" + maxExclusive + ")");
        }
        this.minInclusive = minInclusive;
        this.maxExclusive = maxExclusive;
    }

    @Override
    public double applyAsDouble(OpContext opContext) {
        return opContext.random().nextDouble(minInclusive, maxExclusive);
    }
}

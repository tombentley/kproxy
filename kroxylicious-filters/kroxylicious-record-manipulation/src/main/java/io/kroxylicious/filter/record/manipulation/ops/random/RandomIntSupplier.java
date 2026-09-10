/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.record.manipulation.ops.random;

import java.util.function.ToIntFunction;

import io.kroxylicious.filter.record.manipulation.op.OpContext;

/**
 * A function that returns an {@code int} drawn at random from a range.
 */
class RandomIntSupplier implements ToIntFunction<OpContext> {
    private final int minInclusive;
    private final int maxExclusive;

    /**
     * Creates an instance.
     * @param minInclusive the minimum value (inclusive)
     * @param maxExclusive the maximum value (exclusive)
     */
    RandomIntSupplier(int minInclusive, int maxExclusive) {
        if (minInclusive >= maxExclusive) {
            throw new IllegalArgumentException("minInclusive (" + minInclusive + ") must be < maxExclusive (" + maxExclusive + ")");
        }
        this.minInclusive = minInclusive;
        this.maxExclusive = maxExclusive;
    }

    @Override
    public int applyAsInt(OpContext opContext) {
        return opContext.random().nextInt(minInclusive, maxExclusive);
    }
}

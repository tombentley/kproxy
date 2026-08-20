/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.record.manipulation.common;

import java.util.Random;
import java.util.function.DoubleSupplier;

public class RandomDoubleSupplier implements DoubleSupplier {
    private final Random prng;
    private final double minInclusive;
    private final double maxExclusive;

    public RandomDoubleSupplier(Random prng, double minInclusive, double maxExclusive) {
        if (minInclusive >= maxExclusive) {
            throw new IllegalArgumentException("minInclusive (" + minInclusive + ") must be < maxExclusive (" + maxExclusive + ")");
        }
        this.prng = prng;
        this.minInclusive = minInclusive;
        this.maxExclusive = maxExclusive;
    }

    @Override
    public double getAsDouble() {
        return prng.nextDouble(minInclusive, maxExclusive);
    }
}

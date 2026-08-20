/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.record.manipulation.common;

import java.util.Random;
import java.util.function.IntSupplier;

public class RandomIntSupplier implements IntSupplier {
    private final Random prng;
    private final int minInclusive;
    private final int maxExclusive;

    public RandomIntSupplier(Random prng, int minInclusive, int maxExclusive) {
        if (minInclusive >= maxExclusive) {
            throw new IllegalArgumentException("minInclusive (" + minInclusive + ") must be < maxExclusive (" + maxExclusive + ")");
        }
        this.prng = prng;
        this.minInclusive = minInclusive;
        this.maxExclusive = maxExclusive;
    }

    @Override
    public int getAsInt() {
        return prng.nextInt(minInclusive, maxExclusive);
    }
}

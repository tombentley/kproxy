/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.record.manipulation.common;

import java.util.Random;
import java.util.function.LongSupplier;

public class RandomLongSupplier implements LongSupplier {
    private final Random prng;
    private final int minInclusive;
    private final int maxExclusive;

    public RandomLongSupplier(Random prng, int minInclusive, int maxExclusive) {
        this.prng = prng;
        this.minInclusive = minInclusive;
        this.maxExclusive = maxExclusive;
    }

    @Override
    public long getAsLong() {
        return prng.nextLong(minInclusive, maxExclusive);
    }
}

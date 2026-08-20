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
    private final int min;
    private final int max;

    public RandomIntSupplier(Random prng, int min, int max) {
        this.prng = prng;
        this.min = min;
        this.max = max;
    }

    @Override
    public int getAsInt() {
        return prng.nextInt(min, max + 1);
    }
}

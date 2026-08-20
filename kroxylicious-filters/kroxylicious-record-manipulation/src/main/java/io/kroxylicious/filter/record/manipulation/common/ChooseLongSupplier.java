/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.record.manipulation.common;

import java.util.Random;
import java.util.Set;
import java.util.function.LongSupplier;

public class ChooseLongSupplier implements LongSupplier {
    private final Random prng;
    private final long[] values;

    public ChooseLongSupplier(Random prng, Set<Long> from) {
        this.prng = prng;
        values = from.stream().mapToLong(i -> i).toArray();
    }

    @Override
    public long getAsLong() {
        int index = prng.nextInt(0, values.length);
        return values[index];
    }
}

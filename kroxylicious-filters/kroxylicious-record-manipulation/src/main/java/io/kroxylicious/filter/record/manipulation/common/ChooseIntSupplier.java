/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.record.manipulation.common;

import java.util.Random;
import java.util.Set;
import java.util.function.IntSupplier;

public class ChooseIntSupplier implements IntSupplier {
    private final Random prng;
    private final int[] values;

    public ChooseIntSupplier(Random prng, Set<Integer> from) {
        this.prng = prng;
        values = from.stream().mapToInt(i -> i).toArray();
    }

    @Override
    public int getAsInt() {
        int index = prng.nextInt(0, values.length);
        return values[index];
    }
}

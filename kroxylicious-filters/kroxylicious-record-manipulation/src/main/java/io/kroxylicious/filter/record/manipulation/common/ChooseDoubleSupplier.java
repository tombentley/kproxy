/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.record.manipulation.common;

import java.util.Random;
import java.util.Set;
import java.util.function.DoubleSupplier;

public class ChooseDoubleSupplier implements DoubleSupplier {
    private final Random prng;
    private final double[] values;

    public ChooseDoubleSupplier(Random prng, Set<Double> from) {
        this.prng = prng;
        values = from.stream().mapToDouble(i -> i).toArray();
    }

    @Override
    public double getAsDouble() {
        int index = prng.nextInt(0, values.length);
        return values[index];
    }
}

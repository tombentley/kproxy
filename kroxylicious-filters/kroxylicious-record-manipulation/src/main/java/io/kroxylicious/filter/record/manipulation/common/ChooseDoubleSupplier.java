/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.record.manipulation.common;

import java.util.Set;
import java.util.function.ToDoubleFunction;

/**
 * A function that returns a {@code double} drawn at random from a fixed set.
 */
public class ChooseDoubleSupplier implements ToDoubleFunction<Context> {
    private final double[] values;

    /**
     * Creates an instance.
     * @param from the set of values to choose from
     */
    public ChooseDoubleSupplier(Set<Double> from) {
        values = from.stream().mapToDouble(i -> i).toArray();
    }

    @Override
    public double applyAsDouble(Context context) {
        int index = context.random().nextInt(0, values.length);
        return values[index];
    }
}

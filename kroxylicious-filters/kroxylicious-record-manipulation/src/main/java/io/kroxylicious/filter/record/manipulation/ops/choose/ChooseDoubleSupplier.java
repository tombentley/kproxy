/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.record.manipulation.ops.choose;

import java.util.Set;
import java.util.function.BiFunction;

import io.kroxylicious.filter.record.manipulation.op.OpContext;

/**
 * A function that returns a {@code double} drawn at random from a fixed set.
 */
class ChooseDoubleSupplier implements BiFunction<Object, OpContext, Double> {
    private final double[] values;

    /**
     * Creates an instance.
     * @param from the set of values to choose from
     */
    ChooseDoubleSupplier(Set<Double> from) {
        values = from.stream().mapToDouble(i -> i).toArray();
    }

    @Override
    public Double apply(Object ignored, OpContext opContext) {
        int index = opContext.random().nextInt(0, values.length);
        return values[index];
    }
}

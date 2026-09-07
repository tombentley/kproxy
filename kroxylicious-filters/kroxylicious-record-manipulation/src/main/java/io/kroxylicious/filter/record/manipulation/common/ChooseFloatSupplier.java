/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.record.manipulation.common;

import java.util.Set;
import java.util.function.Function;

import io.kroxylicious.filter.record.manipulation.op.OpContext;

/**
 * A function that returns a {@code double} drawn at random from a fixed set.
 */
public class ChooseFloatSupplier implements Function<OpContext, Float> {

    private final float[] values;

    /**
     * Creates an instance.
     * @param from the set of values to choose from
     */
    public ChooseFloatSupplier(Set<Float> from) {
        values = new float[from.size()];
        var iter = from.iterator();
        for (int i = 0; i < values.length; i++) {
            values[i] = iter.next();
        }
    }

    @Override
    public Float apply(OpContext opContext) {
        int index = opContext.random().nextInt(0, values.length);
        return values[index];
    }
}

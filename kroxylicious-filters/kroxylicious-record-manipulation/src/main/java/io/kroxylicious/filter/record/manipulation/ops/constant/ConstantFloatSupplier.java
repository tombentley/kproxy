/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.record.manipulation.ops.constant;

import java.util.function.Function;

import io.kroxylicious.filter.record.manipulation.op.OpContext;

/**
 * A function that always returns the same {@code double}, regardless of context.
 */
class ConstantFloatSupplier implements Function<OpContext, Float> {
    private final float value;

    /**
     * Creates an instance.
     * @param value the value to always return
     */
    ConstantFloatSupplier(float value) {
        this.value = value;
    }

    @Override
    public Float apply(OpContext opContext) {
        return value;
    }
}

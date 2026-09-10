/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.record.manipulation.ops.constant;

import java.util.function.ToDoubleFunction;

import io.kroxylicious.filter.record.manipulation.op.OpContext;

/**
 * A function that always returns the same {@code double}, regardless of context.
 */
class ConstantDoubleSupplier implements ToDoubleFunction<OpContext> {
    private final double value;

    /**
     * Creates an instance.
     * @param value the value to always return
     */
    ConstantDoubleSupplier(double value) {
        this.value = value;
    }

    @Override
    public double applyAsDouble(OpContext opContext) {
        return value;
    }
}

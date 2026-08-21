/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.record.manipulation.common;

import java.util.function.ToDoubleFunction;

/**
 * A function that always returns the same {@code double}, regardless of context.
 */
public class ConstantDoubleSupplier implements ToDoubleFunction<Context> {
    private final double value;

    /**
     * Creates an instance.
     * @param value the value to always return
     */
    public ConstantDoubleSupplier(double value) {
        this.value = value;
    }

    @Override
    public double applyAsDouble(Context context) {
        return value;
    }
}

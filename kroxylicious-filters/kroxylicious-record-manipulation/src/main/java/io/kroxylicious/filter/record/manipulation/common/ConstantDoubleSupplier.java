/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.record.manipulation.common;

import java.util.function.DoubleSupplier;

/**
 * A supplier that always returns the same {@code double}.
 */
public class ConstantDoubleSupplier implements DoubleSupplier {
    private final double value;

    /**
     * Creates a supplier.
     * @param value the value to always return
     */
    public ConstantDoubleSupplier(double value) {
        this.value = value;
    }

    @Override
    public double getAsDouble() {
        return value;
    }
}

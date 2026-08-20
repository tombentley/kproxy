/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.record.manipulation.common;

import java.util.function.DoubleSupplier;

public class ConstantDoubleSupplier implements DoubleSupplier {
    private final double value;

    public ConstantDoubleSupplier(double value) {
        this.value = value;
    }

    @Override
    public double getAsDouble() {
        return value;
    }
}

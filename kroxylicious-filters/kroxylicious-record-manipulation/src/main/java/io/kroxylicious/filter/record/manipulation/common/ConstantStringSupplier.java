/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.record.manipulation.common;

import java.util.function.Supplier;

/**
 * A supplier that always returns the same {@link String}.
 */
public class ConstantStringSupplier implements Supplier<String> {
    private final String value;

    /**
     * Creates a supplier.
     * @param value the value to always return
     */
    public ConstantStringSupplier(String value) {
        this.value = value;
    }

    @Override
    public String get() {
        return value;
    }
}

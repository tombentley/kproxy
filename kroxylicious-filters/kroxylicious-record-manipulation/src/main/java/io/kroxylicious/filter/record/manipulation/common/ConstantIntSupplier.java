/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.record.manipulation.common;

import java.util.function.ToIntFunction;

/**
 * A function that always returns the same {@code int}, regardless of context.
 */
public class ConstantIntSupplier implements ToIntFunction<Context> {
    private final int value;

    /**
     * Creates an instance.
     * @param value the value to always return
     */
    public ConstantIntSupplier(int value) {
        this.value = value;
    }

    @Override
    public int applyAsInt(Context context) {
        return value;
    }
}

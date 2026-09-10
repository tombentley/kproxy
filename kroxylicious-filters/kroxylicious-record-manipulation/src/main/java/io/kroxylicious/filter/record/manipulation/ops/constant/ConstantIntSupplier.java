/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.record.manipulation.ops.constant;

import java.util.function.ToIntFunction;

import io.kroxylicious.filter.record.manipulation.op.OpContext;

/**
 * A function that always returns the same {@code int}, regardless of context.
 */
class ConstantIntSupplier implements ToIntFunction<OpContext> {
    private final int value;

    /**
     * Creates an instance.
     * @param value the value to always return
     */
    ConstantIntSupplier(int value) {
        this.value = value;
    }

    @Override
    public int applyAsInt(OpContext opContext) {
        return value;
    }
}

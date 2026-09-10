/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.record.manipulation.ops.constant;

import java.util.function.ToLongFunction;

import io.kroxylicious.filter.record.manipulation.op.OpContext;

/**
 * A function that always returns the same {@code long}, regardless of context.
 */
class ConstantLongSupplier implements ToLongFunction<OpContext> {
    private final long value;

    /**
     * Creates an instance.
     * @param value the value to always return
     */
    ConstantLongSupplier(long value) {
        this.value = value;
    }

    @Override
    public long applyAsLong(OpContext opContext) {
        return value;
    }
}

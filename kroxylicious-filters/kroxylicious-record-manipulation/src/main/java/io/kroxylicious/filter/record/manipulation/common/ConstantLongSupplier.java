/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.record.manipulation.common;

import java.util.function.ToLongFunction;

/**
 * A function that always returns the same {@code long}, regardless of context.
 */
public class ConstantLongSupplier implements ToLongFunction<Context> {
    private final long value;

    /**
     * Creates an instance.
     * @param value the value to always return
     */
    public ConstantLongSupplier(long value) {
        this.value = value;
    }

    @Override
    public long applyAsLong(Context context) {
        return value;
    }
}

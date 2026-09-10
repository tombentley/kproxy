/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.record.manipulation.ops.constant;

import java.util.function.Function;

import io.kroxylicious.filter.record.manipulation.op.OpContext;

/**
 * A function that always returns the same {@link String}, regardless of context.
 */
class ConstantStringSupplier implements Function<OpContext, String> {
    private final String value;

    /**
     * Creates an instance.
     * @param value the value to always return
     */
    ConstantStringSupplier(String value) {
        this.value = value;
    }

    @Override
    public String apply(OpContext opContext) {
        return value;
    }
}

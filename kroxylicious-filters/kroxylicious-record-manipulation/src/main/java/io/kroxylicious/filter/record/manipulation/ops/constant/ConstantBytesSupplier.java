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
public class ConstantBytesSupplier implements Function<OpContext, byte[]> {
    private final byte[] value;

    /**
     * Creates an instance.
     * @param value the value to always return
     */
    public ConstantBytesSupplier(byte[] value) {
        this.value = value;
    }

    @Override
    public byte[] apply(OpContext opContext) {
        return value;
    }
}

/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.record.manipulation.common;

import java.math.BigInteger;
import java.util.function.Function;

/**
 * A function that always returns the same {@code BigInteger}, regardless of context.
 */
public class ConstantBigIntegerSupplier implements Function<OpContext, BigInteger> {
    private final BigInteger value;

    /**
     * Creates an instance.
     * @param value the value to always return
     */
    public ConstantBigIntegerSupplier(BigInteger value) {
        this.value = value;
    }

    @Override
    public BigInteger apply(OpContext opContext) {
        return value;
    }
}

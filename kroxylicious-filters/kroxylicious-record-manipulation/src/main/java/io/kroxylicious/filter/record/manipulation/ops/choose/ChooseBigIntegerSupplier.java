/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.record.manipulation.ops.choose;

import java.math.BigInteger;
import java.util.Set;
import java.util.function.BiFunction;

import io.kroxylicious.filter.record.manipulation.op.OpContext;

/**
 * A function that returns a {@code BigInteger} drawn at random from a fixed set.
 */
class ChooseBigIntegerSupplier implements BiFunction<BigInteger, OpContext, BigInteger> {
    private final BigInteger[] values;

    /**
     * Creates an instance.
     * @param from the set of values to choose from
     */
    ChooseBigIntegerSupplier(Set<BigInteger> from) {
        values = from.stream().toArray(BigInteger[]::new);
    }

    @Override
    public BigInteger apply(BigInteger ignored, OpContext opContext) {
        int index = opContext.random().nextInt(0, values.length);
        return values[index];
    }
}

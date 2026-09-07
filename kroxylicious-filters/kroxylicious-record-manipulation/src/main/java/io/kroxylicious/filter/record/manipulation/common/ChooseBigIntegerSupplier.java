/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.record.manipulation.common;

import java.math.BigInteger;
import java.util.Set;
import java.util.function.BiFunction;

/**
 * A function that returns a {@code BigInteger} drawn at random from a fixed set.
 */
public class ChooseBigIntegerSupplier implements BiFunction<BigInteger, OpContext, BigInteger> {
    private final BigInteger[] values;

    /**
     * Creates an instance.
     * @param from the set of values to choose from
     */
    public ChooseBigIntegerSupplier(Set<BigInteger> from) {
        values = from.stream().toArray(BigInteger[]::new);
    }

    @Override
    public BigInteger apply(BigInteger ignored, OpContext opContext) {
        int index = opContext.random().nextInt(0, values.length);
        return values[index];
    }
}

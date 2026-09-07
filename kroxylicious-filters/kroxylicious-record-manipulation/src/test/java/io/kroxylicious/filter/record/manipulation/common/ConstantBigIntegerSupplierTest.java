/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.record.manipulation.common;

import java.math.BigInteger;
import java.util.Random;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ConstantBigIntegerSupplierTest {

    private static final OpContext OP_CONTEXT = new OpContext(new Random(), new byte[0]);

    @Test
    void returnsConfiguredValueEveryTime() {
        // Given
        ConstantBigIntegerSupplier supplier = new ConstantBigIntegerSupplier(BigInteger.valueOf(42));

        // When
        BigInteger first = supplier.apply(OP_CONTEXT);
        BigInteger second = supplier.apply(OP_CONTEXT);

        // Then
        assertThat(first).isEqualTo(BigInteger.valueOf(42));
        assertThat(second).isEqualTo(BigInteger.valueOf(42));
    }

    @Test
    void supportsValuesOutsideTheLongRange() {
        // Given
        BigInteger huge = BigInteger.valueOf(Long.MAX_VALUE).add(BigInteger.ONE);
        ConstantBigIntegerSupplier supplier = new ConstantBigIntegerSupplier(huge);

        // When
        BigInteger value = supplier.apply(OP_CONTEXT);

        // Then
        assertThat(value).isEqualTo(huge);
    }

}

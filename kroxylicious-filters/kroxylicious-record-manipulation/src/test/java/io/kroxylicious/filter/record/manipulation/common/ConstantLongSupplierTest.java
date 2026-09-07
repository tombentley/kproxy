/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.record.manipulation.common;

import java.util.Random;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ConstantLongSupplierTest {

    private static final OpContext OP_CONTEXT = new OpContext(new Random(), new byte[0]);

    @Test
    void returnsConfiguredValueEveryTime() {
        // Given
        ConstantLongSupplier supplier = new ConstantLongSupplier(42);

        // When
        long first = supplier.applyAsLong(OP_CONTEXT);
        long second = supplier.applyAsLong(OP_CONTEXT);

        // Then
        assertThat(first).isEqualTo(42L);
        assertThat(second).isEqualTo(42L);
    }

    @Test
    void supportsValuesOutsideTheIntRange() {
        // Given
        ConstantLongSupplier supplier = new ConstantLongSupplier(Long.MAX_VALUE);

        // When
        long value = supplier.applyAsLong(OP_CONTEXT);

        // Then
        assertThat(value).isEqualTo(Long.MAX_VALUE);
    }

}

/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.record.manipulation.common;

import java.util.Random;

import org.junit.jupiter.api.Test;

import io.kroxylicious.filter.record.manipulation.op.OpContext;

import static org.assertj.core.api.Assertions.assertThat;

class ConstantBooleanSupplierTest {

    private static final OpContext OP_CONTEXT = new OpContext(new Random(), new byte[0]);

    @Test
    void returnsConfiguredValueEveryTime() {
        // Given
        ConstantBooleanSupplier supplier = new ConstantBooleanSupplier(true);

        // When
        boolean first = supplier.test(OP_CONTEXT);
        boolean second = supplier.test(OP_CONTEXT);

        // Then
        assertThat(first).isTrue();
        assertThat(second).isTrue();
    }

    @Test
    void supportsAConfiguredFalseValue() {
        // Given
        ConstantBooleanSupplier supplier = new ConstantBooleanSupplier(false);

        // When
        boolean value = supplier.test(OP_CONTEXT);

        // Then
        assertThat(value).isFalse();
    }

}

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

class ConstantDoubleSupplierTest {

    private static final OpContext OP_CONTEXT = new OpContext(new Random(), new byte[0]);

    @Test
    void returnsConfiguredValueEveryTime() {
        // Given
        ConstantDoubleSupplier supplier = new ConstantDoubleSupplier(3.14);

        // When
        double first = supplier.applyAsDouble(OP_CONTEXT);
        double second = supplier.applyAsDouble(OP_CONTEXT);

        // Then
        assertThat(first).isEqualTo(3.14);
        assertThat(second).isEqualTo(3.14);
    }

}

/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.record.manipulation.common;

import java.util.Random;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ConstantIntSupplierTest {

    private static final OpContext OP_CONTEXT = new OpContext(new Random(), new byte[0]);

    @Test
    void returnsConfiguredValueEveryTime() {
        // Given
        ConstantIntSupplier supplier = new ConstantIntSupplier(42);

        // When
        int first = supplier.applyAsInt(OP_CONTEXT);
        int second = supplier.applyAsInt(OP_CONTEXT);

        // Then
        assertThat(first).isEqualTo(42);
        assertThat(second).isEqualTo(42);
    }

}

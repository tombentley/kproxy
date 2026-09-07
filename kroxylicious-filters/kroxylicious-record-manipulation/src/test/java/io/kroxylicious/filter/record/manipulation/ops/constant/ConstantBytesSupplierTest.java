/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.record.manipulation.ops.constant;

import java.util.Random;

import org.junit.jupiter.api.Test;

import io.kroxylicious.filter.record.manipulation.op.OpContext;

import static org.assertj.core.api.Assertions.assertThat;

class ConstantBytesSupplierTest {

    private static final OpContext OP_CONTEXT = new OpContext(new Random(), new byte[0]);

    @Test
    void returnsConfiguredValueEveryTime() {
        // Given
        byte[] configured = { 1, 2, 3 };
        ConstantBytesSupplier supplier = new ConstantBytesSupplier(configured);

        // When
        byte[] first = supplier.apply(OP_CONTEXT);
        byte[] second = supplier.apply(OP_CONTEXT);

        // Then
        assertThat(first).isEqualTo(configured);
        assertThat(second).isEqualTo(configured);
    }

}

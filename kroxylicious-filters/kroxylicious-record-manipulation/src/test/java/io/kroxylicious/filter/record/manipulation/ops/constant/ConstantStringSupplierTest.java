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

class ConstantStringSupplierTest {

    private static final OpContext OP_CONTEXT = new OpContext(new Random(), new byte[0]);

    @Test
    void returnsConfiguredValueEveryTime() {
        // Given
        ConstantStringSupplier supplier = new ConstantStringSupplier("REDACTED");

        // When
        String first = supplier.apply(OP_CONTEXT);
        String second = supplier.apply(OP_CONTEXT);

        // Then
        assertThat(first).isEqualTo("REDACTED");
        assertThat(second).isEqualTo("REDACTED");
    }

}

/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.record.manipulation.common;

import java.util.Random;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ConstantFloatSupplierTest {

    private static final Context CONTEXT = new Context(new Random(), new byte[0]);

    @Test
    void returnsConfiguredValueEveryTime() {
        // Given
        ConstantFloatSupplier supplier = new ConstantFloatSupplier(3.14f);

        // When
        Float first = supplier.apply(CONTEXT);
        Float second = supplier.apply(CONTEXT);

        // Then
        assertThat(first).isEqualTo(3.14f);
        assertThat(second).isEqualTo(3.14f);
    }

}

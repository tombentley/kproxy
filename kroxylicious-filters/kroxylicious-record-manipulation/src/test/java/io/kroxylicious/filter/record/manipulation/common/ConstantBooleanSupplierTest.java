/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.record.manipulation.common;

import java.util.Random;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ConstantBooleanSupplierTest {

    private static final Context CONTEXT = new Context(new Random(), new byte[0]);

    @Test
    void returnsConfiguredValueEveryTime() {
        // Given
        ConstantBooleanSupplier supplier = new ConstantBooleanSupplier(true);

        // When
        boolean first = supplier.test(CONTEXT);
        boolean second = supplier.test(CONTEXT);

        // Then
        assertThat(first).isTrue();
        assertThat(second).isTrue();
    }

    @Test
    void supportsAConfiguredFalseValue() {
        // Given
        ConstantBooleanSupplier supplier = new ConstantBooleanSupplier(false);

        // When
        boolean value = supplier.test(CONTEXT);

        // Then
        assertThat(value).isFalse();
    }

}

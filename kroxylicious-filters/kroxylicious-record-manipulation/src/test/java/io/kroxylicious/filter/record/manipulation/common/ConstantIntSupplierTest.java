/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.record.manipulation.common;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ConstantIntSupplierTest {

    @Test
    void returnsConfiguredValueEveryTime() {
        // Given
        ConstantIntSupplier supplier = new ConstantIntSupplier(42);

        // When
        int first = supplier.getAsInt();
        int second = supplier.getAsInt();

        // Then
        assertThat(first).isEqualTo(42);
        assertThat(second).isEqualTo(42);
    }

}

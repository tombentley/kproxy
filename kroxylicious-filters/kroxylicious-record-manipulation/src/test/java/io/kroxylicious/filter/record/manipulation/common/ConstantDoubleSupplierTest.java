/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.record.manipulation.common;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ConstantDoubleSupplierTest {

    @Test
    void returnsConfiguredValueEveryTime() {
        // Given
        ConstantDoubleSupplier supplier = new ConstantDoubleSupplier(3.14);

        // When
        double first = supplier.getAsDouble();
        double second = supplier.getAsDouble();

        // Then
        assertThat(first).isEqualTo(3.14);
        assertThat(second).isEqualTo(3.14);
    }

}

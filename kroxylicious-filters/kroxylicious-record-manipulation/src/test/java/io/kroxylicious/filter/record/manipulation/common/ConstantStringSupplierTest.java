/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.record.manipulation.common;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ConstantStringSupplierTest {

    @Test
    void returnsConfiguredValueEveryTime() {
        // Given
        ConstantStringSupplier supplier = new ConstantStringSupplier("REDACTED");

        // When
        String first = supplier.get();
        String second = supplier.get();

        // Then
        assertThat(first).isEqualTo("REDACTED");
        assertThat(second).isEqualTo("REDACTED");
    }

}

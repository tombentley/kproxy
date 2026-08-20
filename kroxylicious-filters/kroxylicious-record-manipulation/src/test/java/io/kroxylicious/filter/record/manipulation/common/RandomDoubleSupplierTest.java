/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.record.manipulation.common;

import java.util.Random;
import java.util.stream.DoubleStream;
import java.util.stream.IntStream;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

class RandomDoubleSupplierTest {

    @Test
    void valuesFallWithinRange() {
        // Given
        RandomDoubleSupplier supplier = new RandomDoubleSupplier(new Random(0), 10.0, 20.0);

        // When
        double[] values = IntStream.range(0, 500).mapToDouble(i -> supplier.getAsDouble()).toArray();

        // Then
        assertThat(DoubleStream.of(values).allMatch(value -> value >= 10.0 && value < 20.0)).isTrue();
    }

    @Test
    void rejectsMinGreaterThanMax() {
        // Given
        Random prng = new Random();

        // When/Then
        assertThatIllegalArgumentException().isThrownBy(() -> new RandomDoubleSupplier(prng, 10.0, 5.0));
    }

    @Test
    void rejectsMinEqualToMax() {
        // Given
        Random prng = new Random();

        // When/Then
        assertThatIllegalArgumentException().isThrownBy(() -> new RandomDoubleSupplier(prng, 5.0, 5.0));
    }

}

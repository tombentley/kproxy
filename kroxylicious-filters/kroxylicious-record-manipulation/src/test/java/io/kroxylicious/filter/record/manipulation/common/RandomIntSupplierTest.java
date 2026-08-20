/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.record.manipulation.common;

import java.util.Random;
import java.util.stream.IntStream;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

class RandomIntSupplierTest {

    @Test
    void maxIsExclusive() {
        // Given
        RandomIntSupplier supplier = new RandomIntSupplier(new Random(), 5, 6);

        // When
        int value = supplier.getAsInt();

        // Then
        assertThat(value).isEqualTo(5);
    }

    @Test
    void valuesFallWithinRange() {
        // Given
        RandomIntSupplier supplier = new RandomIntSupplier(new Random(0), 10, 20);

        // When
        int[] values = IntStream.range(0, 500).map(i -> supplier.getAsInt()).toArray();

        // Then
        assertThat(IntStream.of(values).allMatch(value -> value >= 10 && value < 20)).isTrue();
    }

    @Test
    void supportsMaxAtIntegerMaxValueWithoutOverflow() {
        // Given
        RandomIntSupplier supplier = new RandomIntSupplier(new Random(0), Integer.MAX_VALUE - 1, Integer.MAX_VALUE);

        // When
        int value = supplier.getAsInt();

        // Then
        assertThat(value).isEqualTo(Integer.MAX_VALUE - 1);
    }

    @Test
    void rejectsMinGreaterThanMax() {
        // Given
        Random prng = new Random();

        // When/Then
        assertThatIllegalArgumentException().isThrownBy(() -> new RandomIntSupplier(prng, 10, 5));
    }

    @Test
    void rejectsMinEqualToMax() {
        // Given
        Random prng = new Random();

        // When/Then
        assertThatIllegalArgumentException().isThrownBy(() -> new RandomIntSupplier(prng, 5, 5));
    }

}

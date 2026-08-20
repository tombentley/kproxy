/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.record.manipulation.common;

import java.util.Random;
import java.util.stream.LongStream;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

class RandomLongSupplierTest {

    @Test
    void maxIsExclusive() {
        // Given
        RandomLongSupplier supplier = new RandomLongSupplier(new Random(), 5, 6);

        // When
        long value = supplier.getAsLong();

        // Then
        assertThat(value).isEqualTo(5L);
    }

    @Test
    void valuesFallWithinRange() {
        // Given
        RandomLongSupplier supplier = new RandomLongSupplier(new Random(0), 10, 20);

        // When
        long[] values = LongStream.range(0, 500).map(i -> supplier.getAsLong()).toArray();

        // Then
        assertThat(LongStream.of(values).allMatch(value -> value >= 10 && value < 20)).isTrue();
    }

    @Test
    void supportsBoundsOutsideTheIntRange() {
        // Given
        long min = Long.MAX_VALUE - 1;
        RandomLongSupplier supplier = new RandomLongSupplier(new Random(0), min, Long.MAX_VALUE);

        // When
        long value = supplier.getAsLong();

        // Then
        assertThat(value).isEqualTo(min);
    }

    @Test
    void rejectsMinGreaterThanMax() {
        // Given
        Random prng = new Random();

        // When/Then
        assertThatIllegalArgumentException().isThrownBy(() -> new RandomLongSupplier(prng, 10, 5));
    }

    @Test
    void rejectsMinEqualToMax() {
        // Given
        Random prng = new Random();

        // When/Then
        assertThatIllegalArgumentException().isThrownBy(() -> new RandomLongSupplier(prng, 5, 5));
    }

}

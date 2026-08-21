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

    private static Context contextWithSeed(long seed) {
        return new Context(new Random(seed), new byte[0]);
    }

    @Test
    void maxIsExclusive() {
        // Given
        RandomLongSupplier supplier = new RandomLongSupplier(5, 6);

        // When
        long value = supplier.applyAsLong(contextWithSeed(0));

        // Then
        assertThat(value).isEqualTo(5L);
    }

    @Test
    void valuesFallWithinRange() {
        // Given
        RandomLongSupplier supplier = new RandomLongSupplier(10, 20);
        Context context = contextWithSeed(0);

        // When
        long[] values = LongStream.range(0, 500).map(i -> supplier.applyAsLong(context)).toArray();

        // Then
        assertThat(LongStream.of(values).allMatch(value -> value >= 10 && value < 20)).isTrue();
    }

    @Test
    void supportsBoundsOutsideTheIntRange() {
        // Given
        long min = Long.MAX_VALUE - 1;
        RandomLongSupplier supplier = new RandomLongSupplier(min, Long.MAX_VALUE);

        // When
        long value = supplier.applyAsLong(contextWithSeed(0));

        // Then
        assertThat(value).isEqualTo(min);
    }

    @Test
    void rejectsMinGreaterThanMax() {
        // When/Then
        assertThatIllegalArgumentException().isThrownBy(() -> new RandomLongSupplier(10, 5));
    }

    @Test
    void rejectsMinEqualToMax() {
        // When/Then
        assertThatIllegalArgumentException().isThrownBy(() -> new RandomLongSupplier(5, 5));
    }

}

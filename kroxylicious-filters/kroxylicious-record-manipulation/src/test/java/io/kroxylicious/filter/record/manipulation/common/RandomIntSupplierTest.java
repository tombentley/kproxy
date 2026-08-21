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

    private static Context contextWithSeed(long seed) {
        return new Context(new Random(seed), new byte[0]);
    }

    @Test
    void maxIsExclusive() {
        // Given
        RandomIntSupplier supplier = new RandomIntSupplier(5, 6);

        // When
        int value = supplier.applyAsInt(contextWithSeed(0));

        // Then
        assertThat(value).isEqualTo(5);
    }

    @Test
    void valuesFallWithinRange() {
        // Given
        RandomIntSupplier supplier = new RandomIntSupplier(10, 20);
        Context context = contextWithSeed(0);

        // When
        int[] values = IntStream.range(0, 500).map(i -> supplier.applyAsInt(context)).toArray();

        // Then
        assertThat(IntStream.of(values).allMatch(value -> value >= 10 && value < 20)).isTrue();
    }

    @Test
    void supportsMaxAtIntegerMaxValueWithoutOverflow() {
        // Given
        RandomIntSupplier supplier = new RandomIntSupplier(Integer.MAX_VALUE - 1, Integer.MAX_VALUE);

        // When
        int value = supplier.applyAsInt(contextWithSeed(0));

        // Then
        assertThat(value).isEqualTo(Integer.MAX_VALUE - 1);
    }

    @Test
    void rejectsMinGreaterThanMax() {
        // When/Then
        assertThatIllegalArgumentException().isThrownBy(() -> new RandomIntSupplier(10, 5));
    }

    @Test
    void rejectsMinEqualToMax() {
        // When/Then
        assertThatIllegalArgumentException().isThrownBy(() -> new RandomIntSupplier(5, 5));
    }

}

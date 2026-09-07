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

class RandomFloatSupplierTest {

    private static OpContext contextWithSeed(long seed) {
        return new OpContext(new Random(seed), new byte[0]);
    }

    @Test
    void valuesFallWithinRange() {
        // Given
        RandomFloatSupplier supplier = new RandomFloatSupplier(10.0f, 20.0f);
        OpContext opContext = contextWithSeed(0);

        // When
        Float[] values = IntStream.range(0, 500).mapToObj(i -> supplier.apply(opContext)).toArray(Float[]::new);

        // Then
        assertThat(values).allMatch(value -> value >= 10.0f && value < 20.0f);
    }

    @Test
    void rejectsMinGreaterThanMax() {
        // When/Then
        assertThatIllegalArgumentException().isThrownBy(() -> new RandomFloatSupplier(10.0f, 5.0f));
    }

    @Test
    void rejectsMinEqualToMax() {
        // When/Then
        assertThatIllegalArgumentException().isThrownBy(() -> new RandomFloatSupplier(5.0f, 5.0f));
    }

}

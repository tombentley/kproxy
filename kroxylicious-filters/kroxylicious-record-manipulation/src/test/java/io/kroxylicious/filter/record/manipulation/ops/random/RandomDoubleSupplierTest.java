/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.record.manipulation.ops.random;

import java.util.Random;
import java.util.stream.DoubleStream;
import java.util.stream.IntStream;

import org.junit.jupiter.api.Test;

import io.kroxylicious.filter.record.manipulation.op.OpContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

class RandomDoubleSupplierTest {

    private static OpContext contextWithSeed(long seed) {
        return new OpContext(new Random(seed), new byte[0]);
    }

    @Test
    void valuesFallWithinRange() {
        // Given
        RandomDoubleSupplier supplier = new RandomDoubleSupplier(10.0, 20.0);
        OpContext opContext = contextWithSeed(0);

        // When
        double[] values = IntStream.range(0, 500).mapToDouble(i -> supplier.applyAsDouble(opContext)).toArray();

        // Then
        assertThat(DoubleStream.of(values).allMatch(value -> value >= 10.0 && value < 20.0)).isTrue();
    }

    @Test
    void rejectsMinGreaterThanMax() {
        // When/Then
        assertThatIllegalArgumentException().isThrownBy(() -> new RandomDoubleSupplier(10.0, 5.0));
    }

    @Test
    void rejectsMinEqualToMax() {
        // When/Then
        assertThatIllegalArgumentException().isThrownBy(() -> new RandomDoubleSupplier(5.0, 5.0));
    }

}

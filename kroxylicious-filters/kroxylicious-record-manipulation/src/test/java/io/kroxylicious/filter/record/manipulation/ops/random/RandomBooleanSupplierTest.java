/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.record.manipulation.ops.random;

import java.util.Random;
import java.util.stream.IntStream;

import org.junit.jupiter.api.Test;

import io.kroxylicious.filter.record.manipulation.op.OpContext;

import static org.assertj.core.api.Assertions.assertThat;

class RandomBooleanSupplierTest {

    private static OpContext contextWithSeed(long seed) {
        return new OpContext(new Random(seed), new byte[0]);
    }

    @Test
    void drawsBothTrueAndFalseOverManyCalls() {
        // Given
        RandomBooleanSupplier supplier = new RandomBooleanSupplier();
        OpContext opContext = contextWithSeed(0);

        // When
        boolean[] values = new boolean[50];
        IntStream.range(0, values.length).forEach(i -> values[i] = supplier.test(opContext));

        // Then
        assertThat(values).contains(true).contains(false);
    }

}

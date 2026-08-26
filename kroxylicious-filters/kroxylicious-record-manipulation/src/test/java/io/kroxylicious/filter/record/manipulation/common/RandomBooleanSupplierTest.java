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

class RandomBooleanSupplierTest {

    private static Context contextWithSeed(long seed) {
        return new Context(new Random(seed), new byte[0]);
    }

    @Test
    void drawsBothTrueAndFalseOverManyCalls() {
        // Given
        RandomBooleanSupplier supplier = new RandomBooleanSupplier();
        Context context = contextWithSeed(0);

        // When
        boolean[] values = new boolean[50];
        IntStream.range(0, values.length).forEach(i -> values[i] = supplier.test(context));

        // Then
        assertThat(values).contains(true).contains(false);
    }

}

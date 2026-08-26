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

class RandomBytesSupplierTest {

    private static final Context CONTEXT = new Context(new Random(), new byte[0]);

    private static Context contextWithSeed(long seed) {
        return new Context(new Random(seed), new byte[0]);
    }

    @Test
    void maxLengthIsExclusive() {
        // Given
        RandomBytesSupplier supplier = new RandomBytesSupplier(3, 4);

        // When
        byte[] value = supplier.apply(CONTEXT);

        // Then
        assertThat(value).hasSize(3);
    }

    @Test
    void minLengthIsInclusive() {
        // Given
        RandomBytesSupplier supplier = new RandomBytesSupplier(0, 1);

        // When
        byte[] value = supplier.apply(CONTEXT);

        // Then
        assertThat(value).isEmpty();
    }

    @Test
    void lengthFallsWithinConfiguredRange() {
        // Given
        RandomBytesSupplier supplier = new RandomBytesSupplier(3, 15);
        Context context = contextWithSeed(0);

        // When
        int[] lengths = IntStream.range(0, 200).map(i -> supplier.apply(context).length).toArray();

        // Then
        assertThat(IntStream.of(lengths).allMatch(length -> length >= 3 && length < 15)).isTrue();
    }

    @Test
    void rejectsNegativeMinLength() {
        // When/Then
        assertThatIllegalArgumentException().isThrownBy(() -> new RandomBytesSupplier(-1, 5));
    }

    @Test
    void rejectsMinLengthGreaterThanMaxLength() {
        // When/Then
        assertThatIllegalArgumentException().isThrownBy(() -> new RandomBytesSupplier(5, 3));
    }

    @Test
    void rejectsMinLengthEqualToMaxLength() {
        // When/Then
        assertThatIllegalArgumentException().isThrownBy(() -> new RandomBytesSupplier(5, 5));
    }

}

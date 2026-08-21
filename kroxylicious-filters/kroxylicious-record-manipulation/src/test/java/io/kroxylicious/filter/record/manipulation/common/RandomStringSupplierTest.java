/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.record.manipulation.common;

import java.util.Random;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

class RandomStringSupplierTest {

    private static final Context CONTEXT = new Context(new Random(), new byte[0]);

    private static Context contextWithSeed(long seed) {
        return new Context(new Random(seed), new byte[0]);
    }

    @Test
    void maxLengthIsExclusive() {
        // Given
        RandomStringSupplier supplier = new RandomStringSupplier("abc", 3, 4);

        // When
        String value = supplier.apply(CONTEXT);

        // Then
        assertThat(value).hasSize(3);
    }

    @Test
    void minLengthIsInclusive() {
        // Given
        RandomStringSupplier supplier = new RandomStringSupplier("abc", 0, 1);

        // When
        String value = supplier.apply(CONTEXT);

        // Then
        assertThat(value).isEmpty();
    }

    @Test
    void everyCharacterIsDrawnFromAlphabet() {
        // Given
        RandomStringSupplier supplier = new RandomStringSupplier("xyz", 20, 21);

        // When
        String value = supplier.apply(contextWithSeed(0));

        // Then
        assertThat(value.chars().allMatch(c -> c == 'x' || c == 'y' || c == 'z')).isTrue();
    }

    @Test
    void singleCharacterAlphabetProducesRepeatsOfThatCharacter() {
        // Given
        RandomStringSupplier supplier = new RandomStringSupplier("q", 5, 6);

        // When
        String value = supplier.apply(contextWithSeed(0));

        // Then
        assertThat(value).isEqualTo("qqqqq");
    }

    @Test
    void rejectsNegativeMinLength() {
        // When/Then
        assertThatIllegalArgumentException().isThrownBy(() -> new RandomStringSupplier("abc", -1, 5));
    }

    @Test
    void rejectsMinLengthGreaterThanMaxLength() {
        // When/Then
        assertThatIllegalArgumentException().isThrownBy(() -> new RandomStringSupplier("abc", 5, 3));
    }

    @Test
    void rejectsMinLengthEqualToMaxLength() {
        // When/Then
        assertThatIllegalArgumentException().isThrownBy(() -> new RandomStringSupplier("abc", 5, 5));
    }

}

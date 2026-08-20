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

    @Test
    void maxLengthIsExclusive() {
        // Given
        RandomStringSupplier supplier = new RandomStringSupplier(new Random(), "abc", 3, 4);

        // When
        String value = supplier.get();

        // Then
        assertThat(value).hasSize(3);
    }

    @Test
    void minLengthIsInclusive() {
        // Given
        RandomStringSupplier supplier = new RandomStringSupplier(new Random(), "abc", 0, 1);

        // When
        String value = supplier.get();

        // Then
        assertThat(value).isEmpty();
    }

    @Test
    void everyCharacterIsDrawnFromAlphabet() {
        // Given
        RandomStringSupplier supplier = new RandomStringSupplier(new Random(0), "xyz", 20, 21);

        // When
        String value = supplier.get();

        // Then
        assertThat(value.chars().allMatch(c -> c == 'x' || c == 'y' || c == 'z')).isTrue();
    }

    @Test
    void singleCharacterAlphabetProducesRepeatsOfThatCharacter() {
        // Given
        RandomStringSupplier supplier = new RandomStringSupplier(new Random(0), "q", 5, 6);

        // When
        String value = supplier.get();

        // Then
        assertThat(value).isEqualTo("qqqqq");
    }

    @Test
    void rejectsNegativeMinLength() {
        // Given
        Random prng = new Random();

        // When/Then
        assertThatIllegalArgumentException().isThrownBy(() -> new RandomStringSupplier(prng, "abc", -1, 5));
    }

    @Test
    void rejectsMinLengthGreaterThanMaxLength() {
        // Given
        Random prng = new Random();

        // When/Then
        assertThatIllegalArgumentException().isThrownBy(() -> new RandomStringSupplier(prng, "abc", 5, 3));
    }

    @Test
    void rejectsMinLengthEqualToMaxLength() {
        // Given
        Random prng = new Random();

        // When/Then
        assertThatIllegalArgumentException().isThrownBy(() -> new RandomStringSupplier(prng, "abc", 5, 5));
    }

}

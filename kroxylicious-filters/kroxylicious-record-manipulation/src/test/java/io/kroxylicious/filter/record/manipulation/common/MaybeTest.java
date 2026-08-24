/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.record.manipulation.common;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MaybeTest {

    @Test
    void someWrapsTheGivenValue() {
        // Given
        Maybe<String> maybe = Maybe.some("hello");

        // When
        String matched = switch (maybe) {
            case Maybe.Some<String> some -> some.value();
            case Maybe.None<String> none -> "absent";
        };

        // Then
        assertThat(matched).isEqualTo("hello");
    }

    @Test
    void noneRepresentsAbsence() {
        // Given
        Maybe<String> maybe = Maybe.none();

        // When
        String matched = switch (maybe) {
            case Maybe.Some<String> some -> some.value();
            case Maybe.None<String> none -> "absent";
        };

        // Then
        assertThat(matched).isEqualTo("absent");
    }

}

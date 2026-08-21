/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.record.manipulation.common;

import java.util.Random;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class EncryptStringFunctionTest {

    private static final byte[] KEY = { 1, 2, 3, 4, 5, 6, 7, 8, 9, 0, 1, 2, 3, 4, 5, 6 };

    @Test
    void encryptingTheSamePlaintextTwiceProducesDifferentCiphertext() {
        // Given
        EncryptStringFunction encrypt = new EncryptStringFunction(KEY, new Random());

        // When
        String first = encrypt.apply("hello");
        String second = encrypt.apply("hello");

        // Then
        assertThat(first).isNotEqualTo(second);
    }

}

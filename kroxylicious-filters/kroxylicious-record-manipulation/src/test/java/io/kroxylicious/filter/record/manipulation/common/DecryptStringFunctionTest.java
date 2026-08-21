/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.record.manipulation.common;

import java.util.Base64;
import java.util.Random;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DecryptStringFunctionTest {

    private static final byte[] KEY = { 1, 2, 3, 4, 5, 6, 7, 8, 9, 0, 1, 2, 3, 4, 5, 6 };

    @ParameterizedTest
    @ValueSource(strings = { "", "hello world", "unicode: héllo wörld 日本語" })
    void decryptReversesEncrypt(String plaintext) {
        // Given
        EncryptStringFunction encrypt = new EncryptStringFunction(KEY, new Random());
        DecryptStringFunction decrypt = new DecryptStringFunction(KEY);

        // When
        String ciphertext = encrypt.apply(plaintext);
        String roundTripped = decrypt.apply(ciphertext);

        // Then
        assertThat(roundTripped).isEqualTo(plaintext);
    }

    @Test
    void decryptingATamperedCiphertextFails() {
        // Given
        EncryptStringFunction encrypt = new EncryptStringFunction(KEY, new Random());
        DecryptStringFunction decrypt = new DecryptStringFunction(KEY);
        String ciphertext = encrypt.apply("hello");
        byte[] tampered = Base64.getDecoder().decode(ciphertext);
        tampered[0] ^= 0xFF;
        String tamperedCiphertext = Base64.getEncoder().encodeToString(tampered);

        // When/Then
        assertThatThrownBy(() -> decrypt.apply(tamperedCiphertext))
                .isInstanceOf(RuntimeException.class);
    }

}

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
    private static final Context CONTEXT = new Context(new Random(), KEY);

    @ParameterizedTest
    @ValueSource(strings = { "", "hello world", "unicode: héllo wörld 日本語" })
    void decryptReversesEncrypt(String plaintext) {
        // Given
        EncryptStringFunction encrypt = new EncryptStringFunction();
        DecryptStringFunction decrypt = new DecryptStringFunction();

        // When
        String ciphertext = encrypt.apply(plaintext, CONTEXT);
        String roundTripped = decrypt.apply(ciphertext, CONTEXT);

        // Then
        assertThat(roundTripped).isEqualTo(plaintext);
    }

    @Test
    void decryptingATamperedCiphertextFails() {
        // Given
        EncryptStringFunction encrypt = new EncryptStringFunction();
        DecryptStringFunction decrypt = new DecryptStringFunction();
        String ciphertext = encrypt.apply("hello", CONTEXT);
        byte[] tampered = Base64.getDecoder().decode(ciphertext);
        tampered[0] ^= 0xFF;
        String tamperedCiphertext = Base64.getEncoder().encodeToString(tampered);

        // When/Then
        assertThatThrownBy(() -> decrypt.apply(tamperedCiphertext, CONTEXT))
                .isInstanceOf(RuntimeException.class);
    }

}

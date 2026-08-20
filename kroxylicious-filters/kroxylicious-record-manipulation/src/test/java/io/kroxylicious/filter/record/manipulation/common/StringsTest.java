/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.record.manipulation.common;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class StringsTest {

    private static final byte[] KEY = { 1, 2, 3, 4, 5, 6, 7, 8, 9, 0, 1, 2, 3, 4, 5, 6 };

    @Test
    void hmacMatchesIndependentlyComputedHmacSha256() throws Exception {
        // Given
        Strings strings = new Strings(KEY);
        Mac oracle = Mac.getInstance("HmacSHA256");
        oracle.init(new SecretKeySpec(KEY, "HmacSHA256"));
        String expected = Base64.getEncoder().encodeToString(oracle.doFinal("hello".getBytes(StandardCharsets.UTF_8)));

        // When
        String actual = strings.hmac().apply("hello");

        // Then
        assertThat(actual).isEqualTo(expected);
    }

    @Test
    void hmacIsDeterministicForTheSameInput() {
        // Given
        Strings strings = new Strings(KEY);

        // When
        String first = strings.hmac().apply("hello");
        String second = strings.hmac().apply("hello");

        // Then
        assertThat(first).isEqualTo(second);
    }

    @Test
    void hmacDiffersForDifferentInput() {
        // Given
        Strings strings = new Strings(KEY);

        // When
        String helloHmac = strings.hmac().apply("hello");
        String worldHmac = strings.hmac().apply("world");

        // Then
        assertThat(helloHmac).isNotEqualTo(worldHmac);
    }

    @ParameterizedTest
    @ValueSource(strings = { "", "hello world", "unicode: héllo wörld 日本語" })
    void decryptReversesEncrypt(String plaintext) {
        // Given
        Strings strings = new Strings(KEY);

        // When
        String ciphertext = strings.encrypt().apply(plaintext);
        String roundTripped = strings.decrypt().apply(ciphertext);

        // Then
        assertThat(roundTripped).isEqualTo(plaintext);
    }

    @Test
    void encryptingTheSamePlaintextTwiceProducesDifferentCiphertext() {
        // Given
        Strings strings = new Strings(KEY);

        // When
        String first = strings.encrypt().apply("hello");
        String second = strings.encrypt().apply("hello");

        // Then
        assertThat(first).isNotEqualTo(second);
    }

    @Test
    void decryptingATamperedCiphertextFails() {
        // Given
        Strings strings = new Strings(KEY);
        String ciphertext = strings.encrypt().apply("hello");
        byte[] tampered = Base64.getDecoder().decode(ciphertext);
        tampered[0] ^= 0xFF;
        String tamperedCiphertext = Base64.getEncoder().encodeToString(tampered);

        // When/Then
        assertThatThrownBy(() -> strings.decrypt().apply(tamperedCiphertext))
                .isInstanceOf(RuntimeException.class);
    }

}

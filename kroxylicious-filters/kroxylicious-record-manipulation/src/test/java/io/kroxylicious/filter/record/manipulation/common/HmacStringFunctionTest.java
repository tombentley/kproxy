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

import static org.assertj.core.api.Assertions.assertThat;

class HmacStringFunctionTest {

    private static final byte[] KEY = { 1, 2, 3, 4, 5, 6, 7, 8, 9, 0, 1, 2, 3, 4, 5, 6 };

    @Test
    void hmacMatchesIndependentlyComputedHmacSha256() throws Exception {
        // Given
        HmacStringFunction hmac = new HmacStringFunction(KEY);
        Mac oracle = Mac.getInstance("HmacSHA256");
        oracle.init(new SecretKeySpec(KEY, "HmacSHA256"));
        String expected = Base64.getEncoder().encodeToString(oracle.doFinal("hello".getBytes(StandardCharsets.UTF_8)));

        // When
        String actual = hmac.apply("hello");

        // Then
        assertThat(actual).isEqualTo(expected);
    }

    @Test
    void hmacIsDeterministicForTheSameInput() {
        // Given
        HmacStringFunction hmac = new HmacStringFunction(KEY);

        // When
        String first = hmac.apply("hello");
        String second = hmac.apply("hello");

        // Then
        assertThat(first).isEqualTo(second);
    }

    @Test
    void hmacDiffersForDifferentInput() {
        // Given
        HmacStringFunction hmac = new HmacStringFunction(KEY);

        // When
        String helloHmac = hmac.apply("hello");
        String worldHmac = hmac.apply("world");

        // Then
        assertThat(helloHmac).isNotEqualTo(worldHmac);
    }

}

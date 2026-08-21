/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.record.manipulation.common;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Random;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class HmacStringFunctionTest {

    private static final byte[] KEY = { 1, 2, 3, 4, 5, 6, 7, 8, 9, 0, 1, 2, 3, 4, 5, 6 };
    private static final byte[] OTHER_KEY = { 6, 5, 4, 3, 2, 1, 0, 9, 8, 7, 6, 5, 4, 3, 2, 1 };
    private static final Context CONTEXT = new Context(new Random(), KEY);

    @Test
    void hmacMatchesIndependentlyComputedHmacSha256() throws Exception {
        // Given
        HmacStringFunction hmac = new HmacStringFunction();
        Mac oracle = Mac.getInstance("HmacSHA256");
        oracle.init(new SecretKeySpec(KEY, "HmacSHA256"));
        String expected = Base64.getEncoder().encodeToString(oracle.doFinal("hello".getBytes(StandardCharsets.UTF_8)));

        // When
        String actual = hmac.apply("hello", CONTEXT);

        // Then
        assertThat(actual).isEqualTo(expected);
    }

    @Test
    void hmacIsDeterministicForTheSameInput() {
        // Given
        HmacStringFunction hmac = new HmacStringFunction();

        // When
        String first = hmac.apply("hello", CONTEXT);
        String second = hmac.apply("hello", CONTEXT);

        // Then
        assertThat(first).isEqualTo(second);
    }

    @Test
    void hmacDiffersForDifferentInput() {
        // Given
        HmacStringFunction hmac = new HmacStringFunction();

        // When
        String helloHmac = hmac.apply("hello", CONTEXT);
        String worldHmac = hmac.apply("world", CONTEXT);

        // Then
        assertThat(helloHmac).isNotEqualTo(worldHmac);
    }

    @Test
    void oneSharedInstanceHandlesInterleavedDifferentKeysCorrectly() {
        // Given
        HmacStringFunction hmac = new HmacStringFunction();
        Context otherContext = new Context(new Random(), OTHER_KEY);

        // When
        String withFirstKey = hmac.apply("hello", CONTEXT);
        String withOtherKey = hmac.apply("hello", otherContext);
        String withFirstKeyAgain = hmac.apply("hello", CONTEXT);

        // Then
        assertThat(withFirstKey).isNotEqualTo(withOtherKey);
        assertThat(withFirstKeyAgain).isEqualTo(withFirstKey);
    }

}

/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.record.manipulation.config;

import java.util.Base64;
import java.util.Map;
import java.util.Random;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.kroxylicious.filter.record.manipulation.common.BytesOp;
import io.kroxylicious.filter.record.manipulation.common.Context;

import static org.assertj.core.api.Assertions.assertThat;

class ValueBytesTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private static final Context CONTEXT = new Context(new Random(), new byte[0]);

    @Test
    void createsAnOpThatAlwaysReturnsTheConfiguredValue() {
        // Given
        ValueBytes factory = new ValueBytes();
        byte[] configured = { 1, 2, 3 };
        BytesOp op = factory.create(Map.of("value", configured));

        // When
        byte[] value = op.apply(new byte[0], CONTEXT);

        // Then
        assertThat(value).isEqualTo(configured);
    }

    @Test
    void resolvesConfigFromAJsonParsedOpConfigWithABase64EncodedValue() throws Exception {
        // Given
        String base64 = Base64.getEncoder().encodeToString(new byte[]{ 4, 5, 6 });
        OpConfig opConfig = MAPPER.readValue("""
                {"op": "ValueBytes", "value": "%s"}
                """.formatted(base64), OpConfig.class);
        ValueBytes factory = new ValueBytes();
        BytesOp op = factory.create(opConfig.config());

        // When
        byte[] value = op.apply(new byte[0], CONTEXT);

        // Then
        assertThat(value).isEqualTo(new byte[]{ 4, 5, 6 });
    }

    @Test
    void configEqualityIsContentBasedNotIdentityBased() {
        // Given
        ValueBytes.Config first = new ValueBytes.Config(new byte[]{ 1, 2, 3 });
        ValueBytes.Config second = new ValueBytes.Config(new byte[]{ 1, 2, 3 });

        // When/Then
        assertThat(first).isEqualTo(second).hasSameHashCodeAs(second);
    }

    @Test
    void configsWithDifferentValueAreNotEqual() {
        // Given
        ValueBytes.Config first = new ValueBytes.Config(new byte[]{ 1, 2, 3 });
        ValueBytes.Config second = new ValueBytes.Config(new byte[]{ 4, 5, 6 });

        // When/Then
        assertThat(first).isNotEqualTo(second);
    }

    @Test
    void configToStringIncludesTheValueContents() {
        // Given
        ValueBytes.Config config = new ValueBytes.Config(new byte[]{ 1, 2, 3 });

        // When
        String result = config.toString();

        // Then
        assertThat(result).contains("1").contains("2").contains("3");
    }

}

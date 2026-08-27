/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.record.manipulation.config;

import java.util.Map;
import java.util.Random;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.kroxylicious.filter.record.manipulation.common.Context;
import io.kroxylicious.filter.record.manipulation.common.TypedOp;

import static org.assertj.core.api.Assertions.assertThat;

class ValueLongTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private static final Context CONTEXT = new Context(new Random(), new byte[0]);

    @Test
    void createsAnOpThatAlwaysReturnsTheConfiguredValue() {
        // Given
        ValueLong factory = new ValueLong();
        TypedOp<Long, Long> op = factory.create(Map.of("value", 42L));

        // When
        long value = op.apply(0L, CONTEXT);

        // Then
        assertThat(value).isEqualTo(42L);
    }

    @Test
    void resolvesConfigFromAJsonParsedOpConfig() throws Exception {
        // Given
        OpConfig opConfig = MAPPER.readValue("""
                {"op": "ValueLong", "value": 99}
                """, OpConfig.class);
        ValueLong factory = new ValueLong();
        TypedOp<Long, Long> op = factory.create(opConfig.config());

        // When
        long value = op.apply(0L, CONTEXT);

        // Then
        assertThat(value).isEqualTo(99L);
    }

}

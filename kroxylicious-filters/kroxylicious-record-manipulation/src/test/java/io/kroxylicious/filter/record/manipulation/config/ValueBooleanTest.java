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

class ValueBooleanTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private static final Context CONTEXT = new Context(new Random(), new byte[0]);

    @Test
    void createsAnOpThatAlwaysReturnsTheConfiguredValue() {
        // Given
        ValueBoolean factory = new ValueBoolean();
        TypedOp<Boolean, Boolean> op = factory.create(Map.of("value", true));

        // When
        Boolean value = op.apply(false, CONTEXT);

        // Then
        assertThat(value).isTrue();
    }

    @Test
    void resolvesConfigFromAJsonParsedOpConfig() throws Exception {
        // Given
        OpConfig opConfig = MAPPER.readValue("""
                {"op": "ValueBoolean", "value": false}
                """, OpConfig.class);
        ValueBoolean factory = new ValueBoolean();
        TypedOp<Boolean, Boolean> op = factory.create(opConfig.config());

        // When
        Boolean value = op.apply(true, CONTEXT);

        // Then
        assertThat(value).isFalse();
    }

}

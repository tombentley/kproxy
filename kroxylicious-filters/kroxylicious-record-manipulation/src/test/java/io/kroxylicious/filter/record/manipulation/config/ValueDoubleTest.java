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

class ValueDoubleTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private static final Context CONTEXT = new Context(new Random(), new byte[0]);

    @Test
    void createsAnOpThatAlwaysReturnsTheConfiguredValue() {
        // Given
        ValueDouble factory = new ValueDouble();
        TypedOp<Double, Double> op = factory.create(Map.of("value", 3.14));

        // When
        double value = op.apply(0.0, CONTEXT);

        // Then
        assertThat(value).isEqualTo(3.14);
    }

    @Test
    void resolvesConfigFromAJsonParsedOpConfig() throws Exception {
        // Given
        OpConfig opConfig = MAPPER.readValue("""
                {"op": "ValueDouble", "value": 2.71}
                """, OpConfig.class);
        ValueDouble factory = new ValueDouble();
        TypedOp<Double, Double> op = factory.create(opConfig.config());

        // When
        double value = op.apply(0.0, CONTEXT);

        // Then
        assertThat(value).isEqualTo(2.71);
    }

}

/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.record.manipulation.config;

import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.IntStream;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.kroxylicious.filter.record.manipulation.common.Context;
import io.kroxylicious.filter.record.manipulation.common.FloatOp;

import static org.assertj.core.api.Assertions.assertThat;

class ChooseFloatTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private static Context contextWithSeed(long seed) {
        return new Context(new Random(seed), new byte[0]);
    }

    @Test
    void createsAnOpThatDrawsFromTheConfiguredSet() {
        // Given
        ChooseFloat factory = new ChooseFloat();
        FloatOp op = factory.create(Map.of("from", List.of(1.5f, 2.5f, 3.5f)));
        Context context = contextWithSeed(0);

        // When
        Float[] drawn = IntStream.range(0, 200).mapToObj(i -> op.apply(0.0f, context)).toArray(Float[]::new);

        // Then
        assertThat(drawn).allMatch(value -> value == 1.5f || value == 2.5f || value == 3.5f);
    }

    @Test
    void resolvesConfigFromAJsonParsedOpConfig() throws Exception {
        // Given
        OpConfig opConfig = MAPPER.readValue("""
                {"op": "ChooseFloat", "from": [10.0, 20.0, 30.0]}
                """, OpConfig.class);
        ChooseFloat factory = new ChooseFloat();
        FloatOp op = factory.create(opConfig.config());

        // When
        Float value = op.apply(0.0f, contextWithSeed(0));

        // Then
        assertThat(value).isIn(10.0f, 20.0f, 30.0f);
    }

}

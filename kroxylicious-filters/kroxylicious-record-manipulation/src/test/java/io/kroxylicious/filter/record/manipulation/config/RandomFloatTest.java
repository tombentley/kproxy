/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.record.manipulation.config;

import java.util.Map;
import java.util.Random;
import java.util.stream.IntStream;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.kroxylicious.filter.record.manipulation.common.Context;
import io.kroxylicious.filter.record.manipulation.common.FloatOp;

import static org.assertj.core.api.Assertions.assertThat;

class RandomFloatTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private static Context contextWithSeed(long seed) {
        return new Context(new Random(seed), new byte[0]);
    }

    @Test
    void createsAnOpThatDrawsWithinTheConfiguredRange() {
        // Given
        RandomFloat factory = new RandomFloat();
        FloatOp op = factory.create(Map.of("minInclusive", 10.0f, "maxExclusive", 20.0f));
        Context context = contextWithSeed(0);

        // When
        Float[] drawn = IntStream.range(0, 500).mapToObj(i -> op.apply(0.0f, context)).toArray(Float[]::new);

        // Then
        assertThat(drawn).allMatch(value -> value >= 10.0f && value < 20.0f);
    }

    @Test
    void resolvesConfigFromAJsonParsedOpConfig() throws Exception {
        // Given
        OpConfig opConfig = MAPPER.readValue("""
                {"op": "RandomFloat", "minInclusive": 10.0, "maxExclusive": 20.0}
                """, OpConfig.class);
        RandomFloat factory = new RandomFloat();
        FloatOp op = factory.create(opConfig.config());

        // When
        Float value = op.apply(0.0f, contextWithSeed(0));

        // Then
        assertThat(value).isGreaterThanOrEqualTo(10.0f).isLessThan(20.0f);
    }

}

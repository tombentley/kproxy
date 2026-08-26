/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.record.manipulation.config;

import java.util.Map;
import java.util.Random;
import java.util.stream.DoubleStream;
import java.util.stream.IntStream;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.kroxylicious.filter.record.manipulation.common.Context;
import io.kroxylicious.filter.record.manipulation.common.DoubleOp;

import static org.assertj.core.api.Assertions.assertThat;

class RandomDoubleTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private static Context contextWithSeed(long seed) {
        return new Context(new Random(seed), new byte[0]);
    }

    @Test
    void createsAnOpThatDrawsWithinTheConfiguredRange() {
        // Given
        RandomDouble factory = new RandomDouble();
        DoubleOp op = factory.create(Map.of("minInclusive", 10.0, "maxExclusive", 20.0));
        Context context = contextWithSeed(0);

        // When
        double[] drawn = IntStream.range(0, 500).mapToDouble(i -> op.apply(0.0, context)).toArray();

        // Then
        assertThat(DoubleStream.of(drawn).allMatch(value -> value >= 10.0 && value < 20.0)).isTrue();
    }

    @Test
    void resolvesConfigFromAJsonParsedOpConfig() throws Exception {
        // Given
        OpConfig opConfig = MAPPER.readValue("""
                {"op": "RandomDouble", "minInclusive": 10.0, "maxExclusive": 20.0}
                """, OpConfig.class);
        RandomDouble factory = new RandomDouble();
        DoubleOp op = factory.create(opConfig.config());

        // When
        double value = op.apply(0.0, contextWithSeed(0));

        // Then
        assertThat(value).isGreaterThanOrEqualTo(10.0).isLessThan(20.0);
    }

}

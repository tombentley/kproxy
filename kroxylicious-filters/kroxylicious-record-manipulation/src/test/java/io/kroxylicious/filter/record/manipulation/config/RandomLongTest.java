/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.record.manipulation.config;

import java.util.Map;
import java.util.Random;
import java.util.stream.LongStream;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.kroxylicious.filter.record.manipulation.common.Context;
import io.kroxylicious.filter.record.manipulation.common.LongOp;

import static org.assertj.core.api.Assertions.assertThat;

class RandomLongTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private static Context contextWithSeed(long seed) {
        return new Context(new Random(seed), new byte[0]);
    }

    @Test
    void createsAnOpThatDrawsWithinTheConfiguredRange() {
        // Given
        RandomLong factory = new RandomLong();
        LongOp op = factory.create(Map.of("minInclusive", 10L, "maxExclusive", 20L));
        Context context = contextWithSeed(0);

        // When
        long[] drawn = LongStream.range(0, 500).map(i -> op.apply(0L, context)).toArray();

        // Then
        assertThat(LongStream.of(drawn).allMatch(value -> value >= 10L && value < 20L)).isTrue();
    }

    @Test
    void resolvesConfigFromAJsonParsedOpConfig() throws Exception {
        // Given
        OpConfig opConfig = MAPPER.readValue("""
                {"op": "RandomLong", "minInclusive": 10, "maxExclusive": 20}
                """, OpConfig.class);
        RandomLong factory = new RandomLong();
        LongOp op = factory.create(opConfig.config());

        // When
        long value = op.apply(0L, contextWithSeed(0));

        // Then
        assertThat(value).isGreaterThanOrEqualTo(10L).isLessThan(20L);
    }

}

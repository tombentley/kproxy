/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.record.manipulation.config;

import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.LongStream;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.kroxylicious.filter.record.manipulation.common.Context;
import io.kroxylicious.filter.record.manipulation.common.LongOp;

import static org.assertj.core.api.Assertions.assertThat;

class ChooseLongTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private static Context contextWithSeed(long seed) {
        return new Context(new Random(seed), new byte[0]);
    }

    @Test
    void createsAnOpThatDrawsFromTheConfiguredSet() {
        // Given
        ChooseLong factory = new ChooseLong();
        LongOp op = factory.create(Map.of("from", List.of(1L, 2L, 3L)));
        Context context = contextWithSeed(0);

        // When
        long[] drawn = LongStream.range(0, 200).map(i -> op.apply(0L, context)).toArray();

        // Then
        assertThat(LongStream.of(drawn).allMatch(value -> value == 1L || value == 2L || value == 3L)).isTrue();
    }

    @Test
    void resolvesConfigFromAJsonParsedOpConfig() throws Exception {
        // Given
        OpConfig opConfig = MAPPER.readValue("""
                {"op": "ChooseLong", "from": [10, 20, 30]}
                """, OpConfig.class);
        ChooseLong factory = new ChooseLong();
        LongOp op = factory.create(opConfig.config());

        // When
        long value = op.apply(0L, contextWithSeed(0));

        // Then
        assertThat(value).isIn(10L, 20L, 30L);
    }

}

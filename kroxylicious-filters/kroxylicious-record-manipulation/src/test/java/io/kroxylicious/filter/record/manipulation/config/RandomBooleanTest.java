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

import io.kroxylicious.filter.record.manipulation.common.BooleanOp;
import io.kroxylicious.filter.record.manipulation.common.Context;

import static org.assertj.core.api.Assertions.assertThat;

class RandomBooleanTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private static Context contextWithSeed(long seed) {
        return new Context(new Random(seed), new byte[0]);
    }

    @Test
    void createsAnOpThatDrawsBothTrueAndFalseOverManyCalls() {
        // Given
        RandomBoolean factory = new RandomBoolean();
        BooleanOp op = factory.create(Map.of());
        Context context = contextWithSeed(0);

        // When
        Boolean[] drawn = IntStream.range(0, 50).mapToObj(i -> op.apply(false, context)).toArray(Boolean[]::new);

        // Then
        assertThat(drawn).contains(true).contains(false);
    }

    @Test
    void resolvesConfigFromAJsonParsedOpConfig() throws Exception {
        // Given
        OpConfig opConfig = MAPPER.readValue("""
                {"op": "RandomBoolean"}
                """, OpConfig.class);
        RandomBoolean factory = new RandomBoolean();
        BooleanOp op = factory.create(opConfig.config());

        // When
        Boolean value = op.apply(false, contextWithSeed(0));

        // Then
        assertThat(value).isNotNull();
    }

}

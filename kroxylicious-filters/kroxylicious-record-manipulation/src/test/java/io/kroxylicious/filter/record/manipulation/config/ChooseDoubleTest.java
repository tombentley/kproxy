/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.record.manipulation.config;

import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.DoubleStream;
import java.util.stream.IntStream;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.kroxylicious.filter.record.manipulation.common.Context;
import io.kroxylicious.filter.record.manipulation.common.TypedOp;

import static org.assertj.core.api.Assertions.assertThat;

class ChooseDoubleTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private static Context contextWithSeed(long seed) {
        return new Context(new Random(seed), new byte[0]);
    }

    @Test
    void createsAnOpThatDrawsFromTheConfiguredSet() {
        // Given
        ChooseDouble factory = new ChooseDouble();
        TypedOp<Double, Double> op = factory.create(Map.of("from", List.of(1.5, 2.5, 3.5)));
        Context context = contextWithSeed(0);

        // When
        double[] drawn = IntStream.range(0, 200).mapToDouble(i -> op.apply(0.0, context)).toArray();

        // Then
        assertThat(DoubleStream.of(drawn).allMatch(value -> value == 1.5 || value == 2.5 || value == 3.5)).isTrue();
    }

    @Test
    void resolvesConfigFromAJsonParsedOpConfig() throws Exception {
        // Given
        OpConfig opConfig = MAPPER.readValue("""
                {"op": "ChooseDouble", "from": [10.0, 20.0, 30.0]}
                """, OpConfig.class);
        ChooseDouble factory = new ChooseDouble();
        TypedOp<Double, Double> op = factory.create(opConfig.config());

        // When
        double value = op.apply(0.0, contextWithSeed(0));

        // Then
        assertThat(value).isIn(10.0, 20.0, 30.0);
    }

}

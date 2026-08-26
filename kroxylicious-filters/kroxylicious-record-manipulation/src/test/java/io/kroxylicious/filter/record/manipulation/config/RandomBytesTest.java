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

import io.kroxylicious.filter.record.manipulation.common.BytesOp;
import io.kroxylicious.filter.record.manipulation.common.Context;

import static org.assertj.core.api.Assertions.assertThat;

class RandomBytesTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private static Context contextWithSeed(long seed) {
        return new Context(new Random(seed), new byte[0]);
    }

    @Test
    void createsAnOpThatDrawsLengthsWithinTheConfiguredRange() {
        // Given
        RandomBytes factory = new RandomBytes();
        BytesOp op = factory.create(Map.of("minLengthInclusive", 3, "maxLengthExclusive", 15));
        Context context = contextWithSeed(0);

        // When
        int[] lengths = IntStream.range(0, 200).map(i -> op.apply(new byte[0], context).length).toArray();

        // Then
        assertThat(IntStream.of(lengths).allMatch(length -> length >= 3 && length < 15)).isTrue();
    }

    @Test
    void resolvesConfigFromAJsonParsedOpConfig() throws Exception {
        // Given
        OpConfig opConfig = MAPPER.readValue("""
                {"op": "RandomBytes", "minLengthInclusive": 3, "maxLengthExclusive": 4}
                """, OpConfig.class);
        RandomBytes factory = new RandomBytes();
        BytesOp op = factory.create(opConfig.config());

        // When
        byte[] value = op.apply(new byte[0], contextWithSeed(0));

        // Then
        assertThat(value).hasSize(3);
    }

}

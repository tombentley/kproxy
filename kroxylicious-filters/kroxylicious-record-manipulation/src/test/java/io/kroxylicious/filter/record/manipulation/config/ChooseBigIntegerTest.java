/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.record.manipulation.config;

import java.math.BigInteger;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.IntStream;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.kroxylicious.filter.record.manipulation.common.Context;
import io.kroxylicious.filter.record.manipulation.common.TypedOp;

import static org.assertj.core.api.Assertions.assertThat;

class ChooseBigIntegerTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private static Context contextWithSeed(long seed) {
        return new Context(new Random(seed), new byte[0]);
    }

    @Test
    void createsAnOpThatDrawsFromTheConfiguredSet() {
        // Given
        ChooseBigInteger factory = new ChooseBigInteger();
        TypedOp<BigInteger, BigInteger> op = factory.create(Map.of("from", List.of(BigInteger.ONE, BigInteger.TWO, BigInteger.TEN)));
        Context context = contextWithSeed(0);

        // When
        BigInteger[] drawn = IntStream.range(0, 200).mapToObj(i -> op.apply(BigInteger.ZERO, context)).toArray(BigInteger[]::new);

        // Then
        assertThat(drawn).allMatch(value -> value.equals(BigInteger.ONE) || value.equals(BigInteger.TWO) || value.equals(BigInteger.TEN));
    }

    @Test
    void resolvesConfigFromAJsonParsedOpConfig() throws Exception {
        // Given
        OpConfig opConfig = MAPPER.readValue("""
                {"op": "ChooseBigInteger", "from": ["123456789012345678901234567890", "1"]}
                """, OpConfig.class);
        ChooseBigInteger factory = new ChooseBigInteger();
        TypedOp<BigInteger, BigInteger> op = factory.create(opConfig.config());

        // When
        BigInteger value = op.apply(BigInteger.ZERO, contextWithSeed(0));

        // Then
        assertThat(value).isIn(new BigInteger("123456789012345678901234567890"), BigInteger.ONE);
    }

}

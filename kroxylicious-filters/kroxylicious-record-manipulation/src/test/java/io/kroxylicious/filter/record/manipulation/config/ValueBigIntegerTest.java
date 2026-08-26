/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.record.manipulation.config;

import java.math.BigInteger;
import java.util.Map;
import java.util.Random;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.kroxylicious.filter.record.manipulation.common.BigIntegerOp;
import io.kroxylicious.filter.record.manipulation.common.Context;

import static org.assertj.core.api.Assertions.assertThat;

class ValueBigIntegerTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private static final Context CONTEXT = new Context(new Random(), new byte[0]);

    @Test
    void createsAnOpThatAlwaysReturnsTheConfiguredValue() {
        // Given
        ValueBigInteger factory = new ValueBigInteger();
        BigIntegerOp op = factory.create(Map.of("value", BigInteger.valueOf(42)));

        // When
        BigInteger value = op.apply(BigInteger.ZERO, CONTEXT);

        // Then
        assertThat(value).isEqualTo(BigInteger.valueOf(42));
    }

    @Test
    void resolvesConfigFromAJsonParsedOpConfigWithAValueOutsideTheLongRange() throws Exception {
        // Given
        OpConfig opConfig = MAPPER.readValue("""
                {"op": "ValueBigInteger", "value": "123456789012345678901234567890"}
                """, OpConfig.class);
        ValueBigInteger factory = new ValueBigInteger();
        BigIntegerOp op = factory.create(opConfig.config());

        // When
        BigInteger value = op.apply(BigInteger.ZERO, CONTEXT);

        // Then
        assertThat(value).isEqualTo(new BigInteger("123456789012345678901234567890"));
    }

}

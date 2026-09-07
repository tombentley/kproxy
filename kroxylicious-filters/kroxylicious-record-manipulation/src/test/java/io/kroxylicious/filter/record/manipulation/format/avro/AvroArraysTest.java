/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.record.manipulation.format.avro;

import java.util.List;
import java.util.Random;

import org.junit.jupiter.api.Test;

import io.kroxylicious.filter.record.manipulation.op.BaseTypedOp;
import io.kroxylicious.filter.record.manipulation.op.OpContext;

import static org.assertj.core.api.Assertions.assertThat;

class AvroArraysTest {

    private static final OpContext OP_CONTEXT = new OpContext(new Random(), new byte[0]);
    private static final BaseTypedOp<Object, Object> INCREMENT_FN = BaseTypedOp.of(Object.class, Object.class, (value, context) -> (Integer) value + 1);

    @Test
    void itemsAppliesTheFunctionToEachElementInOrder() {
        // Given
        List<Object> input = List.of(1, 2, 3);

        // When
        List<Object> result = AvroArrays.items(INCREMENT_FN).apply(input, OP_CONTEXT);

        // Then
        assertThat(result).containsExactly(2, 3, 4);
    }

    @Test
    void itemsDoesNotMutateTheInputList() {
        // Given
        List<Object> input = List.of(1, 2, 3);

        // When
        var unused = AvroArrays.items(INCREMENT_FN).apply(input, OP_CONTEXT);

        // Then
        assertThat(input).containsExactly(1, 2, 3);
    }

    @Test
    void itemsOnAnEmptyListReturnsAnEmptyList() {
        // Given
        List<Object> input = List.of();

        // When
        List<Object> result = AvroArrays.items(INCREMENT_FN).apply(input, OP_CONTEXT);

        // Then
        assertThat(result).isEmpty();
    }

}

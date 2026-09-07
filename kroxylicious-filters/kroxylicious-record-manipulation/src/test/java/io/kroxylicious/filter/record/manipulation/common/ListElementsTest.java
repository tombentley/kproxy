/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.record.manipulation.common;

import java.util.List;
import java.util.Random;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ListElementsTest {

    private static final OpContext OP_CONTEXT = new OpContext(new Random(), new byte[0]);

    @Test
    void getAllReturnsTheElementsInOrder() {
        // Given
        List<Object> list = List.of(1, 2, 3);

        // When
        List<Object> result = new ListElements().getAll(list);

        // Then
        assertThat(result).containsExactly(1, 2, 3);
    }

//    @Test
//    void modifyAllAppliesTheFunctionToEveryElement() {
//        // Given
//        List<Object> list = List.of(1, 2, 3);
//        BaseTypedOp<Object, Object> incrementFn = (value, context) -> (Integer) value + 1;
//
//        // When
//        List<Object> result = new ListElements().modifyAll(list, incrementFn, OP_CONTEXT);
//
//        // Then
//        assertThat(result).containsExactly(2, 3, 4);
//    }
//
//    @Test
//    void modifyAllDoesNotMutateTheInputList() {
//        // Given
//        List<Object> list = List.of(1, 2, 3);
//        BaseTypedOp<Object, Object> incrementFn = (value, context) -> (Integer) value + 1;
//
//        // When
//        var unused = new ListElements().modifyAll(list, incrementFn, OP_CONTEXT);
//
//        // Then
//        assertThat(list).containsExactly(1, 2, 3);
//    }

}

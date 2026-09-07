/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.record.manipulation.jackson;

import java.util.List;
import java.util.Random;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.IntNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;

import io.kroxylicious.filter.record.manipulation.op.OpContext;

import static org.assertj.core.api.Assertions.assertThat;

class ArrayItemsTest {

    private static final OpContext OP_CONTEXT = new OpContext(new Random(), new byte[0]);

    @Test
    void getAllReturnsTheElementsInOrder() {
        // Given
        ArrayNode array = JsonNodeFactory.instance.arrayNode();
        array.add(1).add(2).add(3);

        // When
        List<JsonNode> result = new ArrayItems().getAll(array);

        // Then
        assertThat(result).containsExactly(new IntNode(1), new IntNode(2), new IntNode(3));
    }

//    @Test
//    void modifyAllAppliesTheFunctionToEveryElement() {
//        // Given
//        ArrayNode array = JsonNodeFactory.instance.arrayNode();
//        array.add(1).add(2).add(3);
//        BaseTypedOp<JsonNode, JsonNode> incrementFn = (node, context) -> new IntNode(node.asInt() + 1);
//
//        // When
//        ArrayNode result = new ArrayItems().modifyAll(array, incrementFn, OP_CONTEXT);
//
//        // Then
//        assertThat(result).isEqualTo(JsonNodeFactory.instance.arrayNode().add(2).add(3).add(4));
//    }
//
//    @Test
//    void modifyAllDoesNotMutateTheInputArray() {
//        // Given
//        ArrayNode array = JsonNodeFactory.instance.arrayNode();
//        array.add(1).add(2).add(3);
//        BaseTypedOp<JsonNode, JsonNode> incrementFn = (node, context) -> new IntNode(node.asInt() + 1);
//
//        // When
//        var unused = new ArrayItems().modifyAll(array, incrementFn, OP_CONTEXT);
//
//        // Then
//        assertThat(array).isEqualTo(JsonNodeFactory.instance.arrayNode().add(1).add(2).add(3));
//    }

}

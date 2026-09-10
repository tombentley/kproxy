/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.record.manipulation.format.jackson2;

import java.util.Random;

import io.kroxylicious.filter.record.manipulation.op.OpContext;

class ArrayNodesTest {

    private static final OpContext OP_CONTEXT = new OpContext(new Random(), new byte[0]);

    // @Test
    // void itemsAppliesTheFunctionToEachElementInOrder() {
    // // Given
    // ArrayNode input = JsonNodeFactory.instance.arrayNode();
    // input.add(1).add(2).add(3);
    // BaseTypedOp<JsonNode, JsonNode> incrementFn = (node, context) -> new IntNode(node.asInt() + 1);
    //
    // // When
    // JsonNode result = ArrayNodes.items(incrementFn).apply(input, OP_CONTEXT);
    //
    // // Then
    // assertThat(result).isEqualTo(JsonNodeFactory.instance.arrayNode().add(2).add(3).add(4));
    // }
    //
    // @Test
    // void itemsDoesNotMutateTheInputArray() {
    // // Given
    // ArrayNode input = JsonNodeFactory.instance.arrayNode();
    // input.add(1).add(2).add(3);
    // BaseTypedOp<JsonNode, JsonNode> incrementFn = (node, context) -> new IntNode(node.asInt() + 1);
    //
    // // When
    // var unused = ArrayNodes.items(incrementFn).apply(input, OP_CONTEXT);
    //
    // // Then
    // assertThat(input).isEqualTo(JsonNodeFactory.instance.arrayNode().add(1).add(2).add(3));
    // }
    //
    // @Test
    // void itemsOnAnEmptyArrayReturnsAnEmptyArray() {
    // // Given
    // ArrayNode input = JsonNodeFactory.instance.arrayNode();
    // BaseTypedOp<JsonNode, JsonNode> incrementFn = (node, context) -> new IntNode(node.asInt() + 1);
    //
    // // When
    // JsonNode result = ArrayNodes.items(incrementFn).apply(input, OP_CONTEXT);
    //
    // // Then
    // assertThat(result).isEqualTo(JsonNodeFactory.instance.arrayNode());
    // }

}

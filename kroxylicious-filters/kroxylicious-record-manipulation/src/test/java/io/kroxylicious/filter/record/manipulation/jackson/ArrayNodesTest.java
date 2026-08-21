/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.record.manipulation.jackson;

import java.util.Random;
import java.util.function.BiFunction;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.IntNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;

import io.kroxylicious.filter.record.manipulation.common.Context;

import static org.assertj.core.api.Assertions.assertThat;

class ArrayNodesTest {

    private static final Context CONTEXT = new Context(new Random(), new byte[0]);

    @Test
    void itemsAppliesTheFunctionToEachElementInOrder() {
        // Given
        ArrayNode input = JsonNodeFactory.instance.arrayNode();
        input.add(1).add(2).add(3);
        BiFunction<JsonNode, Context, JsonNode> incrementFn = (node, context) -> new IntNode(node.asInt() + 1);

        // When
        JsonNode result = ArrayNodes.items(incrementFn).apply(input, CONTEXT);

        // Then
        assertThat(result).isEqualTo(JsonNodeFactory.instance.arrayNode().add(2).add(3).add(4));
    }

    @Test
    void itemsDoesNotMutateTheInputArray() {
        // Given
        ArrayNode input = JsonNodeFactory.instance.arrayNode();
        input.add(1).add(2).add(3);
        BiFunction<JsonNode, Context, JsonNode> incrementFn = (node, context) -> new IntNode(node.asInt() + 1);

        // When
        var unused = ArrayNodes.items(incrementFn).apply(input, CONTEXT);

        // Then
        assertThat(input).isEqualTo(JsonNodeFactory.instance.arrayNode().add(1).add(2).add(3));
    }

    @Test
    void itemsOnAnEmptyArrayReturnsAnEmptyArray() {
        // Given
        ArrayNode input = JsonNodeFactory.instance.arrayNode();
        BiFunction<JsonNode, Context, JsonNode> incrementFn = (node, context) -> new IntNode(node.asInt() + 1);

        // When
        JsonNode result = ArrayNodes.items(incrementFn).apply(input, CONTEXT);

        // Then
        assertThat(result).isEqualTo(JsonNodeFactory.instance.arrayNode());
    }

}

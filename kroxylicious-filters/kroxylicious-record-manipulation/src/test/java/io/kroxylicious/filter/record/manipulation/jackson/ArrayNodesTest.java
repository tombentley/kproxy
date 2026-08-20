/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.record.manipulation.jackson;

import java.util.function.Function;
import java.util.function.Supplier;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.IntNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;

import static org.assertj.core.api.Assertions.assertThat;

class ArrayNodesTest {

    private final ArrayNodes arrayNodes = new ArrayNodes(JsonNodeFactory.instance);

    @Test
    void itemsAppliesTheFunctionToEachElementInOrder() {
        // Given
        ArrayNode input = JsonNodeFactory.instance.arrayNode();
        input.add(1).add(2).add(3);
        Function<JsonNode, JsonNode> incrementFn = node -> new IntNode(node.asInt() + 1);

        // When
        JsonNode result = arrayNodes.items(incrementFn).apply(input);

        // Then
        assertThat(result).isEqualTo(JsonNodeFactory.instance.arrayNode().add(2).add(3).add(4));
    }

    @Test
    void itemsDoesNotMutateTheInputArray() {
        // Given
        ArrayNode input = JsonNodeFactory.instance.arrayNode();
        input.add(1).add(2).add(3);
        Function<JsonNode, JsonNode> incrementFn = node -> new IntNode(node.asInt() + 1);

        // When
        var unused = arrayNodes.items(incrementFn).apply(input);

        // Then
        assertThat(input).isEqualTo(JsonNodeFactory.instance.arrayNode().add(1).add(2).add(3));
    }

    @Test
    void itemsOnAnEmptyArrayReturnsAnEmptyArray() {
        // Given
        ArrayNode input = JsonNodeFactory.instance.arrayNode();
        Function<JsonNode, JsonNode> incrementFn = node -> new IntNode(node.asInt() + 1);

        // When
        JsonNode result = arrayNodes.items(incrementFn).apply(input);

        // Then
        assertThat(result).isEqualTo(JsonNodeFactory.instance.arrayNode());
    }

    @Test
    void items2GeneratesAnArrayPopulatedFromTheSupplier() {
        // Given
        Supplier<JsonNode> constantSupplier = () -> new IntNode(7);

        // When
        JsonNode result = arrayNodes.items2(constantSupplier).get();

        // Then
        assertThat(result).isEqualTo(JsonNodeFactory.instance.arrayNode().add(7).add(7));
    }

}

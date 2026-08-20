/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.record.manipulation.jackson;

import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.IntNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;

import static org.assertj.core.api.Assertions.assertThat;

class ObjectNodesTest {

    private final ObjectNodes objectNodes = new ObjectNodes(JsonNodeFactory.instance);

    @Test
    void mapPropertiesReplacesOnlyThePropertiesPresentInTheMap() {
        // Given
        ObjectNode input = JsonNodeFactory.instance.objectNode();
        input.set("a", new IntNode(1));
        input.set("b", new IntNode(2));
        input.set("c", new IntNode(3));
        Function<JsonNode, JsonNode> incrementFn = node -> new IntNode(node.asInt() + 1);

        // When
        ObjectNode result = objectNodes.mapProperties(Map.of("a", incrementFn)).apply(input);

        // Then
        assertThat(result.get("a")).isEqualTo(new IntNode(2));
        assertThat(result.get("b")).isEqualTo(new IntNode(2));
        assertThat(result.get("c")).isEqualTo(new IntNode(3));
    }

    @Test
    void mapPropertiesMutatesAndReturnsTheSameObjectInstance() {
        // Given
        ObjectNode input = JsonNodeFactory.instance.objectNode();
        input.set("a", new IntNode(1));
        Function<JsonNode, JsonNode> incrementFn = node -> new IntNode(node.asInt() + 1);

        // When
        ObjectNode result = objectNodes.mapProperties(Map.of("a", incrementFn)).apply(input);

        // Then
        assertThat(result).isSameAs(input);
    }

    @Test
    void mapPropertiesIgnoresMapEntriesForPropertiesNotPresentOnTheObject() {
        // Given
        ObjectNode input = JsonNodeFactory.instance.objectNode();
        input.set("a", new IntNode(1));
        Function<JsonNode, JsonNode> incrementFn = node -> new IntNode(node.asInt() + 1);

        // When
        ObjectNode result = objectNodes.mapProperties(Map.of("a", incrementFn, "z", incrementFn)).apply(input);

        // Then
        assertThat(result.properties()).hasSize(1);
        assertThat(result.get("a")).isEqualTo(new IntNode(2));
    }

    @Test
    void mapProperties2BuildsAnObjectFromTheSuppliedValues() {
        // Given
        Supplier<JsonNode> xSupplier = () -> new TextNode("v1");
        Supplier<JsonNode> ySupplier = () -> new IntNode(5);

        // When
        ObjectNode result = objectNodes.mapProperties2(Map.of("x", xSupplier, "y", ySupplier)).get();

        // Then
        ObjectNode expected = JsonNodeFactory.instance.objectNode();
        expected.set("x", new TextNode("v1"));
        expected.set("y", new IntNode(5));
        assertThat(result).isEqualTo(expected);
    }

}

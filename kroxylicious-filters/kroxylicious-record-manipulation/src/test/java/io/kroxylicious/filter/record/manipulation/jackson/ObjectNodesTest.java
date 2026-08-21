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
import com.fasterxml.jackson.databind.node.MissingNode;
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
    void mapPropertiesDoesNotMutateTheInputObject() {
        // Given
        ObjectNode input = JsonNodeFactory.instance.objectNode();
        input.set("a", new IntNode(1));
        Function<JsonNode, JsonNode> incrementFn = node -> new IntNode(node.asInt() + 1);

        // When
        var unused = objectNodes.mapProperties(Map.of("a", incrementFn)).apply(input);

        // Then
        assertThat(input.get("a")).isEqualTo(new IntNode(1));
    }

    @Test
    void mapPropertiesLeavesAPropertyAbsentWhenItsFunctionDeclinesToInsert() {
        // Given
        ObjectNode input = JsonNodeFactory.instance.objectNode();
        input.set("a", new IntNode(1));
        Function<JsonNode, JsonNode> incrementUnlessMissing = node -> node.isMissingNode() ? node : new IntNode(node.asInt() + 1);

        // When
        ObjectNode result = objectNodes.mapProperties(Map.of("a", incrementUnlessMissing, "z", incrementUnlessMissing)).apply(input);

        // Then
        assertThat(result.properties()).hasSize(1);
        assertThat(result.get("a")).isEqualTo(new IntNode(2));
    }

    @Test
    void mapPropertiesInsertsAPropertyDeclaredInTheMapButAbsentFromTheObject() {
        // Given
        ObjectNode input = JsonNodeFactory.instance.objectNode();
        Function<JsonNode, JsonNode> insertFn = ignored -> new IntNode(42);

        // When
        ObjectNode result = objectNodes.mapProperties(Map.of("z", insertFn)).apply(input);

        // Then
        assertThat(result.get("z")).isEqualTo(new IntNode(42));
    }

    @Test
    void mapPropertiesRemovesAPropertyWhenItsFunctionReturnsMissingNode() {
        // Given
        ObjectNode input = JsonNodeFactory.instance.objectNode();
        input.set("a", new IntNode(1));
        Function<JsonNode, JsonNode> deleteFn = ignored -> MissingNode.getInstance();

        // When
        ObjectNode result = objectNodes.mapProperties(Map.of("a", deleteFn)).apply(input);

        // Then
        assertThat(result.properties()).isEmpty();
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

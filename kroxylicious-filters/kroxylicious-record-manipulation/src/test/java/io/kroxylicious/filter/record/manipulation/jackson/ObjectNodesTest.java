/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.record.manipulation.jackson;

import java.util.Map;
import java.util.Random;
import java.util.function.BiFunction;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.IntNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

import io.kroxylicious.filter.record.manipulation.common.Context;
import io.kroxylicious.filter.record.manipulation.common.Maybe;

import static org.assertj.core.api.Assertions.assertThat;

class ObjectNodesTest {

    private static final Context CONTEXT = new Context(new Random(), new byte[0]);

    private final ObjectNodes objectNodes = new ObjectNodes(JsonNodeFactory.instance);

    @Test
    void mapPropertiesReplacesOnlyThePropertiesPresentInTheMap() {
        // Given
        ObjectNode input = JsonNodeFactory.instance.objectNode();
        input.set("a", new IntNode(1));
        input.set("b", new IntNode(2));
        input.set("c", new IntNode(3));
        BiFunction<Maybe<JsonNode>, Context, Maybe<JsonNode>> incrementFn = (maybe, context) -> Maybe
                .some(new IntNode(((Maybe.Some<JsonNode>) maybe).value().asInt() + 1));

        // When
        ObjectNode result = objectNodes.mapProperties(Map.of("a", incrementFn)).apply(input, CONTEXT);

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
        BiFunction<Maybe<JsonNode>, Context, Maybe<JsonNode>> incrementFn = (maybe, context) -> Maybe
                .some(new IntNode(((Maybe.Some<JsonNode>) maybe).value().asInt() + 1));

        // When
        var unused = objectNodes.mapProperties(Map.of("a", incrementFn)).apply(input, CONTEXT);

        // Then
        assertThat(input.get("a")).isEqualTo(new IntNode(1));
    }

    @Test
    void mapPropertiesLeavesAPropertyAbsentWhenItsFunctionDeclinesToInsert() {
        // Given
        ObjectNode input = JsonNodeFactory.instance.objectNode();
        input.set("a", new IntNode(1));
        BiFunction<Maybe<JsonNode>, Context, Maybe<JsonNode>> incrementUnlessMissing = (maybe, context) -> maybe instanceof Maybe.Some<JsonNode> some
                ? Maybe.some(new IntNode(some.value().asInt() + 1))
                : Maybe.none();

        // When
        ObjectNode result = objectNodes.mapProperties(Map.of("a", incrementUnlessMissing, "z", incrementUnlessMissing)).apply(input, CONTEXT);

        // Then
        assertThat(result.properties()).hasSize(1);
        assertThat(result.get("a")).isEqualTo(new IntNode(2));
    }

    @Test
    void mapPropertiesInsertsAPropertyDeclaredInTheMapButAbsentFromTheObject() {
        // Given
        ObjectNode input = JsonNodeFactory.instance.objectNode();
        BiFunction<Maybe<JsonNode>, Context, Maybe<JsonNode>> insertFn = (ignored, context) -> Maybe.some(new IntNode(42));

        // When
        ObjectNode result = objectNodes.mapProperties(Map.of("z", insertFn)).apply(input, CONTEXT);

        // Then
        assertThat(result.get("z")).isEqualTo(new IntNode(42));
    }

    @Test
    void mapPropertiesRemovesAPropertyWhenItsFunctionReturnsNone() {
        // Given
        ObjectNode input = JsonNodeFactory.instance.objectNode();
        input.set("a", new IntNode(1));
        BiFunction<Maybe<JsonNode>, Context, Maybe<JsonNode>> deleteFn = (ignored, context) -> Maybe.none();

        // When
        ObjectNode result = objectNodes.mapProperties(Map.of("a", deleteFn)).apply(input, CONTEXT);

        // Then
        assertThat(result.properties()).isEmpty();
    }

}

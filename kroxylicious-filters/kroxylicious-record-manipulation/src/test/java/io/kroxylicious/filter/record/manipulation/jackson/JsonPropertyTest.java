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

class JsonPropertyTest {

    private static final Context CONTEXT = new Context(new Random(), new byte[0]);

    @Test
    void getReturnsSomeWhenThePropertyIsPresent() {
        // Given
        ObjectNode object = JsonNodeFactory.instance.objectNode();
        object.set("a", new IntNode(1));

        // When
        Maybe<JsonNode> result = new JsonProperty("a").get(object);

        // Then
        assertThat(result).isEqualTo(Maybe.some(new IntNode(1)));
    }

    @Test
    void getReturnsNoneWhenThePropertyIsAbsent() {
        // Given
        ObjectNode object = JsonNodeFactory.instance.objectNode();

        // When
        Maybe<JsonNode> result = new JsonProperty("a").get(object);

        // Then
        assertThat(result).isEqualTo(Maybe.none());
    }

    @Test
    void setInsertsAPreviouslyAbsentProperty() {
        // Given
        ObjectNode object = JsonNodeFactory.instance.objectNode();

        // When
        ObjectNode result = new JsonProperty("a").set(object, new IntNode(42));

        // Then
        assertThat(result.get("a")).isEqualTo(new IntNode(42));
    }

    @Test
    void setDoesNotMutateTheInputObject() {
        // Given
        ObjectNode object = JsonNodeFactory.instance.objectNode();

        // When
        var unused = new JsonProperty("a").set(object, new IntNode(42));

        // Then
        assertThat(object.has("a")).isFalse();
    }

    @Test
    void clearRemovesAnExistingProperty() {
        // Given
        ObjectNode object = JsonNodeFactory.instance.objectNode();
        object.set("a", new IntNode(1));

        // When
        ObjectNode result = new JsonProperty("a").clear(object);

        // Then
        assertThat(result.has("a")).isFalse();
    }

    @Test
    void clearDoesNotMutateTheInputObject() {
        // Given
        ObjectNode object = JsonNodeFactory.instance.objectNode();
        object.set("a", new IntNode(1));

        // When
        var unused = new JsonProperty("a").clear(object);

        // Then
        assertThat(object.has("a")).isTrue();
    }

    @Test
    void modifyReplacesAPresentValue() {
        // Given
        ObjectNode object = JsonNodeFactory.instance.objectNode();
        object.set("a", new IntNode(1));
        BiFunction<Maybe<JsonNode>, Context, Maybe<JsonNode>> increment = (maybe, context) -> Maybe
                .some(new IntNode(((Maybe.Some<JsonNode>) maybe).value().asInt() + 1));

        // When
        ObjectNode result = new JsonProperty("a").modify(object, increment, CONTEXT);

        // Then
        assertThat(result.get("a")).isEqualTo(new IntNode(2));
    }

    @Test
    void modifyInsertsAnAbsentValue() {
        // Given
        ObjectNode object = JsonNodeFactory.instance.objectNode();
        BiFunction<Maybe<JsonNode>, Context, Maybe<JsonNode>> insertIfAbsent = (maybe, context) -> maybe instanceof Maybe.None<JsonNode>
                ? Maybe.some(new IntNode(42))
                : maybe;

        // When
        ObjectNode result = new JsonProperty("a").modify(object, insertIfAbsent, CONTEXT);

        // Then
        assertThat(result.get("a")).isEqualTo(new IntNode(42));
    }

    @Test
    void modifyDeletesAPresentValue() {
        // Given
        ObjectNode object = JsonNodeFactory.instance.objectNode();
        object.set("a", new IntNode(1));
        BiFunction<Maybe<JsonNode>, Context, Maybe<JsonNode>> delete = (maybe, context) -> Maybe.none();

        // When
        ObjectNode result = new JsonProperty("a").modify(object, delete, CONTEXT);

        // Then
        assertThat(result.has("a")).isFalse();
    }

    @Test
    void modifyAgreesWithObjectNodesMapProperties() {
        // Given
        ObjectNode object = JsonNodeFactory.instance.objectNode();
        object.set("a", new IntNode(1));
        BiFunction<Maybe<JsonNode>, Context, Maybe<JsonNode>> increment = (maybe, context) -> Maybe
                .some(new IntNode(((Maybe.Some<JsonNode>) maybe).value().asInt() + 1));

        // When
        ObjectNode viaProperty = new JsonProperty("a").modify(object, increment, CONTEXT);
        ObjectNode viaMapProperties = new ObjectNodes(JsonNodeFactory.instance).mapProperties(Map.of("a", increment)).apply(object, CONTEXT);

        // Then
        assertThat(viaProperty).isEqualTo(viaMapProperties);
    }

}

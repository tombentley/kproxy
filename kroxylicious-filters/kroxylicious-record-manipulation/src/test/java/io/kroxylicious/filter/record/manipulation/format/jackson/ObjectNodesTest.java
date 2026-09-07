/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.record.manipulation.format.jackson;

import java.util.Random;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;

import io.kroxylicious.filter.record.manipulation.op.OpContext;

class ObjectNodesTest {

    private static final OpContext OP_CONTEXT = new OpContext(new Random(), new byte[0]);

    private final ObjectNodes objectNodes = new ObjectNodes(JsonNodeFactory.instance);

    // @Test
    // void mapPropertiesReplacesOnlyThePropertiesPresentInTheMap() {
    // // Given
    // ObjectNode input = JsonNodeFactory.instance.objectNode();
    // input.set("a", new IntNode(1));
    // input.set("b", new IntNode(2));
    // input.set("c", new IntNode(3));
    // BiFunction<Maybe<JsonNode>, OpContext, Maybe<JsonNode>> incrementFn = (maybe, context) -> Maybe
    // .some(new IntNode(((Maybe.Some<JsonNode>) maybe).value().asInt() + 1));
    //
    // // When
    // ObjectNode result = objectNodes.mapProperties(Map.of("a", incrementFn)).apply(input, OP_CONTEXT);
    //
    // // Then
    // assertThat(result.get("a")).isEqualTo(new IntNode(2));
    // assertThat(result.get("b")).isEqualTo(new IntNode(2));
    // assertThat(result.get("c")).isEqualTo(new IntNode(3));
    // }
    //
    // @Test
    // void mapPropertiesDoesNotMutateTheInputObject() {
    // // Given
    // ObjectNode input = JsonNodeFactory.instance.objectNode();
    // input.set("a", new IntNode(1));
    // BiFunction<Maybe<JsonNode>, OpContext, Maybe<JsonNode>> incrementFn = (maybe, context) -> Maybe
    // .some(new IntNode(((Maybe.Some<JsonNode>) maybe).value().asInt() + 1));
    //
    // // When
    // var unused = objectNodes.mapProperties(Map.of("a", incrementFn)).apply(input, OP_CONTEXT);
    //
    // // Then
    // assertThat(input.get("a")).isEqualTo(new IntNode(1));
    // }
    //
    // @Test
    // void mapPropertiesLeavesAPropertyAbsentWhenItsFunctionDeclinesToInsert() {
    // // Given
    // ObjectNode input = JsonNodeFactory.instance.objectNode();
    // input.set("a", new IntNode(1));
    // BiFunction<Maybe<JsonNode>, OpContext, Maybe<JsonNode>> incrementUnlessMissing = (maybe, context) -> maybe instanceof Maybe.Some<JsonNode> some
    // ? Maybe.some(new IntNode(some.value().asInt() + 1))
    // : Maybe.none();
    //
    // // When
    // ObjectNode result = objectNodes.mapProperties(Map.of("a", incrementUnlessMissing, "z", incrementUnlessMissing)).apply(input, OP_CONTEXT);
    //
    // // Then
    // assertThat(result.properties()).hasSize(1);
    // assertThat(result.get("a")).isEqualTo(new IntNode(2));
    // }
    //
    // @Test
    // void mapPropertiesInsertsAPropertyDeclaredInTheMapButAbsentFromTheObject() {
    // // Given
    // ObjectNode input = JsonNodeFactory.instance.objectNode();
    // BiFunction<Maybe<JsonNode>, OpContext, Maybe<JsonNode>> insertFn = (ignored, context) -> Maybe.some(new IntNode(42));
    //
    // // When
    // ObjectNode result = objectNodes.mapProperties(Map.of("z", insertFn)).apply(input, OP_CONTEXT);
    //
    // // Then
    // assertThat(result.get("z")).isEqualTo(new IntNode(42));
    // }
    //
    // @Test
    // void mapPropertiesRemovesAPropertyWhenItsFunctionReturnsNone() {
    // // Given
    // ObjectNode input = JsonNodeFactory.instance.objectNode();
    // input.set("a", new IntNode(1));
    // BiFunction<Maybe<JsonNode>, OpContext, Maybe<JsonNode>> deleteFn = (ignored, context) -> Maybe.none();
    //
    // // When
    // ObjectNode result = objectNodes.mapProperties(Map.of("a", deleteFn)).apply(input, OP_CONTEXT);
    //
    // // Then
    // assertThat(result.properties()).isEmpty();
    // }

}

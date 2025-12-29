/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.transformation.format.json;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.function.Predicate;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;

import io.kroxylicious.filter.transformation.RecordDataLocation;
import io.kroxylicious.filter.transformation.TransformationInputStream;
import io.kroxylicious.filter.transformation.api.mapper.Context;

import static org.assertj.core.api.Assertions.assertThat;

public class JsonDeserializerTest {

    private JsonDeserializer jsonDeserializer = new JsonDeserializer();

    @Test
    void shouldHaveJsonNodeType() {
        assertThat(jsonDeserializer.returnedType()).isEqualTo(JsonNode.class);
    }

    static List<Arguments> shouldDeserialize() {
        JsonNodeFactory nodeFactory = new ObjectMapper().getNodeFactory();
        return List.of(
                Arguments.argumentSet("null", "null", (Predicate<JsonNode>) JsonNode::isNull),
                Arguments.argumentSet("boolean", "true", (Predicate<JsonNode>) JsonNode::isBoolean),
                Arguments.argumentSet("integer", "42", (Predicate<JsonNode>) JsonNode::isIntegralNumber),
                Arguments.argumentSet("double", "3.141", (Predicate<JsonNode>) JsonNode::isFloatingPointNumber),
                Arguments.argumentSet("string", "\"hello, world\"", (Predicate<JsonNode>) JsonNode::isTextual),
                Arguments.argumentSet("object", "{}", (Predicate<JsonNode>) JsonNode::isObject),
                Arguments.argumentSet("object2", "{\"num\":12}", (Predicate<JsonNode>) JsonNode::isObject),
                Arguments.argumentSet("array", "[]", (Predicate<JsonNode>) JsonNode::isArray),
                Arguments.argumentSet("array2", "[13]", (Predicate<JsonNode>) JsonNode::isArray)
        );
    }

    @ParameterizedTest
    @MethodSource
    void shouldDeserialize(String json, Predicate<Object> assertion) throws IOException {
        TransformationInputStream in = new TransformationInputStream(ByteBuffer.wrap(json.getBytes(StandardCharsets.UTF_8)));
        var value = jsonDeserializer.deserialize(in, new Context("test-topic", List.of(), RecordDataLocation.KEY));
        assertThat((Object) value).matches(assertion);

    }
}

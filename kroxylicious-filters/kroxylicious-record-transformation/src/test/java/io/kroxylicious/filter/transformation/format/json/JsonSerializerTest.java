/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.transformation.format.json;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;

import io.kroxylicious.filter.transformation.TransformationOutputStream;

import static org.assertj.core.api.Assertions.assertThat;

class JsonSerializerTest {

    static List<Arguments> shouldSerialize() {
        JsonNodeFactory nodeFactory = new ObjectMapper().getNodeFactory();
        return List.of(
                Arguments.argumentSet("null", nodeFactory.nullNode(), "null"),
                Arguments.argumentSet("boolean", nodeFactory.booleanNode(true), "true"),
                Arguments.argumentSet("integer", nodeFactory.numberNode(42), "42"),
                Arguments.argumentSet("double", nodeFactory.numberNode(3.141), "3.141"),
                Arguments.argumentSet("string", nodeFactory.textNode("hello, world"), "\"hello, world\""),
                Arguments.argumentSet("object", nodeFactory.objectNode(), "{}"),
                Arguments.argumentSet("object2", nodeFactory.objectNode().put("num", 12), "{\"num\":12}"),
                Arguments.argumentSet("array", nodeFactory.arrayNode(), "[]"),
                Arguments.argumentSet("array2", nodeFactory.arrayNode().add(13), "[13]")
        );
    }

    JsonSerializer jsonSerializer = new JsonSerializer(new ObjectMapper());

    @ParameterizedTest
    @MethodSource
    void shouldSerialize(JsonNode node, String expected) throws IOException {
        // Given
        TransformationOutputStream output = new TransformationOutputStream(100);

        // When
        jsonSerializer.serialize(node, output);

        // Then
        String string = StandardCharsets.UTF_8.newDecoder().decode(output.toByteBuffer()).toString();
        assertThat(string).isEqualTo(expected);
    }

}
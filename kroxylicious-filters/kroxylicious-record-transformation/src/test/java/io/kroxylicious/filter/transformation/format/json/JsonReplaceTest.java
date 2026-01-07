/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.transformation.format.json;

import java.util.List;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import io.kroxylicious.filter.transformation.api.SchemaAndValue;
import io.kroxylicious.filter.transformation.api.schema.identification.NoSchemaId;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JsonReplaceTest {

    ObjectMapper mapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);

    static List<Arguments> shouldRejectInvalidPointer() {
        return List.of(
                Arguments.of( "foo", "Invalid input: JSON Pointer expression must start with '/': \"foo\""),
                Arguments.of( "42", "Invalid input: JSON Pointer expression must start with '/': \"42\""),
                Arguments.of( "\n", "Invalid input: JSON Pointer expression must start with '/': \"\n\"")
        );
    }

    @ParameterizedTest
    @MethodSource
    void shouldRejectInvalidPointer(String pointer, String expectedMessage) throws JsonProcessingException {
        JsonNode replacement1 = mapper.nullNode();
        assertThatThrownBy(() -> new JsonReplace(pointer, replacement1))
                .isExactlyInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining(expectedMessage);

    }

    static List<Arguments> shouldRemove() {
        return List.of(
                Arguments.argumentSet(
                        "replace name in object",
                        "/name",
                        """
                        "***\"""",
                        """
                        {
                          "name" : "John Doe",
                          "userId" : 1234
                        }""",
                        """
                        {
                          "name" : "***",
                          "userId" : 1234
                        }"""),
                Arguments.argumentSet(
                        "replace userId in object",
                        "/userId",
                        """
                        0""",
                        """
                        {
                          "name" : "John Doe",
                          "userId" : 1234
                        }""",
                        """
                        {
                          "name" : "John Doe",
                          "userId" : 0
                        }"""),
                Arguments.argumentSet(
                        "replace notPresentField in object",
                        "/notPresentField",
                        """
                        true""",
                        """
                        {
                          "name" : "John Doe",
                          "userId" : 1234
                        }""",
                        """
                        {
                          "name" : "John Doe",
                          "userId" : 1234
                        }"""),
                Arguments.argumentSet("replace index 0 in array",
                        "/0",
                        """
                        "***\"""",
                        """
                        [ "John Doe", 1234 ]""",
                        """
                        [ "***", 1234 ]"""),
                Arguments.argumentSet("replace index 1 in array",
                        "/1",
                        "0",
                        """
                        [ "John Doe", 1234 ]""",
                        """
                        [ "John Doe", 0 ]"""),
                Arguments.argumentSet("replace last index in array",
                        "/-",
                        "0",
                        """
                        [ "John Doe", 1234 ]""",
                        """
                        [ "John Doe", 0 ]"""),
                Arguments.argumentSet("replace index -1 in array",
                        "/-1",
                        "0",
                        """
                        [ "John Doe", 1234 ]""",
                        """
                        [ "John Doe", 0 ]"""),
                Arguments.argumentSet("remove index 2 in array",
                        "/2", "true",
                        """
                        [ "John Doe", 1234 ]""",
                        """
                        [ "John Doe", 1234 ]"""),
                Arguments.argumentSet("replace root (object)",
                        "/", "true",
                        """
                        {
                          "name" : "John Doe",
                          "userId" : 1234
                        }""",
                        """
                        true"""),
                Arguments.argumentSet("remove root (array)",
                        "/", "true",
                        """
                        [ "John Doe", 1234 ]""",
                        """
                        true"""),
                Arguments.argumentSet("replace root (string)",
                        "/", "true",
                        """
                        "John Doe\"""",
                        """
                        true"""),
                Arguments.argumentSet("replace root (string)",
                        "name", "true",
                        """
                        "John Doe\"""",
                        """
                        true""")
        );
    }

    @ParameterizedTest
    @MethodSource
    void shouldRemove(String pointer, String replacement, String input, String expected) throws JsonProcessingException {
        JsonReplace jsonReplace = new JsonReplace(pointer, mapper.readTree(replacement));
        var node = mapper.readTree(input);
        var result = jsonReplace.transform(new SchemaAndValue<>(NoSchemaId.INSTANCE, null, node), null);
        assertThat(mapper.writeValueAsString(result.value())).isEqualTo(expected);
    }
}
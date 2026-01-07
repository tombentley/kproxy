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

class JsonRemoveTest {

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
                        "remove name from object",
                        "/name",
                        """
                        {
                          "name" : "John Doe",
                          "userId" : 1234
                        }""",
                        """
                        {
                          "userId" : 1234
                        }"""),
                Arguments.argumentSet(
                        "remove userId from object",
                        "/userId",
                        """
                        {
                          "name" : "John Doe",
                          "userId" : 1234
                        }""",
                        """
                        {
                          "name" : "John Doe"
                        }"""),
                Arguments.argumentSet(
                        "remove notPresentField from object",
                        "/notPresentField",
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
                Arguments.argumentSet(
                        "remove index 0 from object",
                        "/0",
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
                Arguments.argumentSet("remove index 0 from two element array",
                        "/0",
                        """
                        [ "John Doe", 1234 ]""",
                        """
                        [ 1234 ]"""),
                // TODO remove entry with key from array
                Arguments.argumentSet("remove index 1 from two element array",
                        "/1",
                        """
                        [ "John Doe", 1234 ]""",
                        """
                        [ "John Doe" ]"""),
                Arguments.argumentSet("remove element after last from two element array",
                        "/-",
                        """
                        [ "John Doe", 1234 ]""",
                        """
                        [ "John Doe", 1234 ]"""),
                Arguments.argumentSet("remove key from two element array",
                        "/foo",
                        """
                        [ "John Doe", 1234 ]""",
                        """
                        [ "John Doe", 1234 ]"""),
                Arguments.argumentSet("remove index 2 from two element array",
                        "/2",
                        """
                        [ "John Doe", 1234 ]""",
                        """
                        [ "John Doe", 1234 ]"""),
                Arguments.argumentSet("remove root (object) is noop",
                        "/",
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
                Arguments.argumentSet("remove root (array) is noop",
                        "/",
                        """
                        [ "John Doe", 1234 ]""",
                        """
                        [ "John Doe", 1234 ]"""),
                Arguments.argumentSet("remove root (string) is noop",
                        "/",
                        """
                        "John Doe\"""",
                        """
                        "John Doe\"""")
        );
    }

    @ParameterizedTest
    @MethodSource
    void shouldRemove(String pointer, String input, String expected) throws JsonProcessingException {
        JsonRemove jsonRemove = new JsonRemove(pointer);
        var node = mapper.readTree(input);
        var result = jsonRemove.transform(new SchemaAndValue<>(NoSchemaId.INSTANCE, null, node), null);
        assertThat(mapper.writeValueAsString(result.value())).isEqualTo(expected);
    }
}
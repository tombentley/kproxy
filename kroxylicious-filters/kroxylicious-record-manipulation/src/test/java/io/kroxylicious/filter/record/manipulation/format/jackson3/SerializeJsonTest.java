/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.record.manipulation.format.jackson3;

import java.lang.reflect.Type;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import io.kroxylicious.filter.record.manipulation.op.OpContext;
import io.kroxylicious.filter.record.manipulation.op.PluginLookup;

import edu.umd.cs.findbugs.annotations.Nullable;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.JsonNodeFactory;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class SerializeJsonTest {

    @Mock
    PluginLookup lookup;
    @Mock
    Type argumentType;
    @Mock
    OpContext opContext;

    static Stream<Arguments> testSerializeJson() {
        return Stream.of(
                Arguments.argumentSet("null", new Object[]{null, JsonNode.class}),
                Arguments.argumentSet("{}", Map.of(), JsonNode.class),
                Arguments.argumentSet("{format: json}", Map.of("format", "json"), JsonNode.class),
                Arguments.argumentSet("{type: Object}", Map.of("type", Object.class.getName()), Object.class),
                Arguments.argumentSet("{format: json, type: Object}", Map.of("format", "json", "type", Object.class.getName()), Object.class)
        );
    }

    @AfterEach
    void tearDown() {
        Mockito.verifyNoInteractions(lookup, argumentType, opContext);
    }

    @ParameterizedTest
    @MethodSource
    void testSerializeJson(@Nullable Map<String, Object> config, Type expectedInputType) {
        JsonNodeFactory jsonNodeFactory = new JsonNodeFactory();
        var op = new SerializeJson().create(config, lookup, argumentType);
        assertThat(op.inputType()).isEqualTo(expectedInputType);
        assertThat(op.outputType()).isEqualTo(ByteBuffer.class);
        ByteBuffer apply = op.apply(jsonNodeFactory.objectNode(), opContext);
        assertThat(StandardCharsets.UTF_8.decode(apply).toString()).isEqualTo("{ }");
    }

    static Stream<Arguments> testSerializeYaml() {
        return Stream.of(
                Arguments.argumentSet("{format: yaml}", Map.of("format", "yaml"), JsonNode.class),
                Arguments.argumentSet("{format: yaml, type: Object}", Map.of("format", "yaml", "type", Object.class.getName()), Object.class)
        );
    }

    @ParameterizedTest
    @MethodSource
    void testSerializeYaml(@Nullable Map<String, Object> config, Type expectedInputType) {
        JsonNodeFactory jsonNodeFactory = new JsonNodeFactory();
        var op = new SerializeJson().create(config, lookup, argumentType);
        assertThat(op.inputType()).isEqualTo(expectedInputType);
        assertThat(op.outputType()).isEqualTo(ByteBuffer.class);
        ByteBuffer apply = op.apply(jsonNodeFactory.objectNode().put("int", 1), opContext);
        assertThat(StandardCharsets.UTF_8.decode(apply).toString()).isEqualTo("""
                ---
                int: 1
                """);
    }

    static Stream<Arguments> testSerializeCsv() {
        return Stream.of(
                Arguments.argumentSet("{format: csv}", Map.of("format", "csv",
                        "columns", List.of(
                                Map.of("name", "foo", "type", "NUMBER"),
                                Map.of("name", "bar", "type", "STRING"))), JsonNode.class),
                Arguments.argumentSet("{format: csv, type: Object}", Map.of("format", "csv",
                        "type", Object.class.getName(),
                        "columns", List.of(
                                Map.of("name", "foo", "type", "NUMBER"),
                                Map.of("name", "bar", "type", "STRING"))), Object.class)
        );
    }

    @ParameterizedTest
    @MethodSource
    void testSerializeCsv(@Nullable Map<String, Object> config, Type expectedInputType) {
        JsonNodeFactory jsonNodeFactory = new JsonNodeFactory();
        var op = new SerializeJson().create(config, lookup, argumentType);
        assertThat(op.inputType()).isEqualTo(expectedInputType);
        assertThat(op.outputType()).isEqualTo(ByteBuffer.class);
        ByteBuffer apply = op.apply(jsonNodeFactory.objectNode().put("foo", 1).put("bar", "food"), opContext);
        assertThat(StandardCharsets.UTF_8.decode(apply).toString()).isEqualTo("""
                1,food
                """);
    }

}
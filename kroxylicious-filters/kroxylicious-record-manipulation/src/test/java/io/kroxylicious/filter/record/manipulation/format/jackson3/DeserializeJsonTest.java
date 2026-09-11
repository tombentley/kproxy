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
import java.util.Set;

import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import io.leangen.geantyref.TypeFactory;

import io.kroxylicious.filter.record.manipulation.op.PluginLookup;
import io.kroxylicious.filter.record.manipulation.format.DeserializationException;
import io.kroxylicious.filter.record.manipulation.op.OpContext;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ObjectNode;
import tools.jackson.dataformat.csv.CsvSchema;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(MockitoExtension.class)
public class DeserializeJsonTest {

    @Mock
    PluginLookup pluginLookup;

    @Mock
    Type argumentType;

    @Mock
    OpContext opContext;

    @AfterEach
    void afterEach() {
        Mockito.verifyNoInteractions(pluginLookup, argumentType, opContext);
    }

    @Test
    void readJsonAsJsonNode() {
        // Given
        Map<String, Object> config = Map.of(DeserializeJson.FORMAT_PARAMETER, DeserializeJson.FORMAT_VALUE_JSON);
        // When
        var op = new DeserializeJson().create(config, pluginLookup, argumentType);
        // Then
        assertThat(op.inputType()).isEqualTo(ByteBuffer.class);
        assertThat(op.outputType()).isEqualTo(JsonNode.class);
        // And When
        Object parsed = op.apply(ByteBuffer.wrap("{\"hello\": \"world\"}".getBytes(StandardCharsets.UTF_8)), opContext);
        // Then
        assertThat(parsed).asInstanceOf(InstanceOfAssertFactories.type(ObjectNode.class)).
                extracting(JsonNode::propertyNames).isEqualTo(Set.of("hello"));
    }

    @Test
    void readJsonWithUnquotedPropertiesAsJsonNodeWhenEnabled() {
        // Given
        Map<String, Object> config = Map.of(DeserializeJson.FORMAT_PARAMETER, DeserializeJson.FORMAT_VALUE_JSON,
                DeserializeJson.READ_FEATURES_PARAMETER, Map.of(
                "ALLOW_UNQUOTED_PROPERTY_NAMES", true));
        // When
        var op = new DeserializeJson().create(config, pluginLookup, argumentType);
        // Then
        assertThat(op.inputType()).isEqualTo(ByteBuffer.class);
        assertThat(op.outputType()).isEqualTo(JsonNode.class);
        // And When
        Object parsed = op.apply(ByteBuffer.wrap("{hello: \"world\"}".getBytes(StandardCharsets.UTF_8)), opContext);
        // Then
        assertThat(parsed).asInstanceOf(InstanceOfAssertFactories.type(ObjectNode.class)).
                extracting(JsonNode::propertyNames).isEqualTo(Set.of("hello"));

    }

    @Test
    void readJsonAsMap() {
        // Given
        Map<String, Object> config = Map.of(
                DeserializeJson.FORMAT_PARAMETER, DeserializeJson.FORMAT_VALUE_JSON,
                DeserializeJson.TYPE_PARAMETER, "java.util.Map");
        // When
        var op = new DeserializeJson().create(config, pluginLookup, argumentType);
        // Then
        assertThat(op.inputType()).isEqualTo(ByteBuffer.class);
        assertThat(op.outputType()).isEqualTo(Map.class);
        // And When
        Object parsed = op.apply(ByteBuffer.wrap("{\"hello\": \"world\"}".getBytes(StandardCharsets.UTF_8)), opContext);
        // Then
        assertThat(parsed).asInstanceOf(InstanceOfAssertFactories.type(Map.class)).
                extracting(Map::keySet).isEqualTo(Set.of("hello"));
    }

    record Greeting(String hello) {}

    @Test
    void readJsonAsJavaClass() {
        // Given
        Map<String, Object> config = Map.of(
                DeserializeJson.FORMAT_PARAMETER, DeserializeJson.FORMAT_VALUE_JSON,
                DeserializeJson.TYPE_PARAMETER, Greeting.class.getName());
        // When
        var op = new DeserializeJson().create(config, pluginLookup, argumentType);
        // Then
        assertThat(op.inputType()).isEqualTo(ByteBuffer.class);
        assertThat(op.outputType()).isEqualTo(Greeting.class);
        // And When
        Object parsed = op.apply(ByteBuffer.wrap("{\"hello\": \"world\"}".getBytes(StandardCharsets.UTF_8)), opContext);
        // Then
        assertThat(parsed).asInstanceOf(InstanceOfAssertFactories.type(Greeting.class)).
                extracting(Greeting::hello).isEqualTo("world");
    }

    @Test
    void cannotReadCsvAsJson() {
        // Given
        Map<String, Object> config = Map.of(DeserializeJson.FORMAT_PARAMETER, DeserializeJson.FORMAT_VALUE_JSON);
        // When
        var op = new DeserializeJson().create(config, pluginLookup, argumentType);
        // Then
        assertThat(op.inputType()).isEqualTo(ByteBuffer.class);
        assertThat(op.outputType()).isEqualTo(JsonNode.class);
        // And When
        ByteBuffer wrap = ByteBuffer.wrap("\"hello\",\"world\"".getBytes(StandardCharsets.UTF_8));
        // Then
        assertThatThrownBy(() -> op.apply(wrap, opContext)).isInstanceOf(DeserializationException.class)
                .hasMessageContaining("Unexpected character (',' (code 44)): expected a value");
    }

    @Test
    void readYamlAsJsonNode() {
        // Given
        Map<String, Object> config = Map.of(DeserializeJson.FORMAT_PARAMETER, DeserializeJson.FORMAT_VALUE_YAML);
        // When
        var op = new DeserializeJson().create(config, pluginLookup, argumentType);
        // Then
        assertThat(op.inputType()).isEqualTo(ByteBuffer.class);
        assertThat(op.outputType()).isEqualTo(JsonNode.class);
        // And When
        Object parsed = op.apply(ByteBuffer.wrap("""
        hello: world
        """.getBytes(StandardCharsets.UTF_8)), opContext);
        // Then
        assertThat(parsed).asInstanceOf(InstanceOfAssertFactories.type(ObjectNode.class)).
                extracting(JsonNode::propertyNames).isEqualTo(Set.of("hello"));
    }

    @Test
    void readYamlAsJavaClass() {
        // Given
        Map<String, Object> config = Map.of(DeserializeJson.FORMAT_PARAMETER, DeserializeJson.FORMAT_VALUE_YAML,
                DeserializeJson.TYPE_PARAMETER, Greeting.class.getName());
        // When
        var op = new DeserializeJson().create(config, pluginLookup, argumentType);
        // Then
        assertThat(op.inputType()).isEqualTo(ByteBuffer.class);
        assertThat(op.outputType()).isEqualTo(Greeting.class);
        // And When
        Object parsed = op.apply(ByteBuffer.wrap("""
        hello: world
        """.getBytes(StandardCharsets.UTF_8)), opContext);
        // Then
        assertThat(parsed).asInstanceOf(InstanceOfAssertFactories.type(Greeting.class)).
                extracting(Greeting::hello).isEqualTo("world");
    }

    @Test
    void readYamlAsListOfJavaClass() {
        // Given
        Map<String, Object> config = Map.of(DeserializeJson.FORMAT_PARAMETER, DeserializeJson.FORMAT_VALUE_YAML,
                DeserializeJson.TYPE_PARAMETER, "java.util.List<" + Greeting.class.getName() + ">");
        // When
        var op = new DeserializeJson().create(config, pluginLookup, argumentType);
        // Then
        assertThat(op.inputType()).isEqualTo(ByteBuffer.class);
        assertThat(op.outputType()).isEqualTo(TypeFactory.parameterizedClass(List.class, Greeting.class));
        // And When
        Object parsed = op.apply(ByteBuffer.wrap("""
        - hello: world
        """.getBytes(StandardCharsets.UTF_8)), opContext);
        // Then
        assertThat(parsed).asInstanceOf(InstanceOfAssertFactories.list(Greeting.class)).
                singleElement().extracting(Greeting::hello).isEqualTo("world");
    }

    @Test
    void readYamlAsArrayOfJavaClass() {
        // Given
        Map<String, Object> config = Map.of(DeserializeJson.FORMAT_PARAMETER, DeserializeJson.FORMAT_VALUE_YAML,
                DeserializeJson.TYPE_PARAMETER, Greeting.class.getName() + "[]");
        // When
        var op = new DeserializeJson().create(config, pluginLookup, argumentType);
        // Then
        assertThat(op.inputType()).isEqualTo(ByteBuffer.class);
        assertThat(op.outputType()).isEqualTo(TypeFactory.parameterizedClass(List.class, Greeting.class));
        // And When
        Object parsed = op.apply(ByteBuffer.wrap("""
        - hello: world
        """.getBytes(StandardCharsets.UTF_8)), opContext);
        // Then
        assertThat(parsed).asInstanceOf(InstanceOfAssertFactories.list(Greeting.class)).
                singleElement().extracting(Greeting::hello).isEqualTo("world");
    }

    record Row(String name, int num) {}

    @Test
    void readCsvAsJsonNode() {
        // Given
        Map<String, Object> config = Map.of(DeserializeJson.FORMAT_PARAMETER, DeserializeJson.FORMAT_VALUE_CSV,
                DeserializeJson.TYPE_PARAMETER, Row.class.getName(),
                "columnConfigs", List.of(
                        new ColumnConfig("name", CsvSchema.ColumnType.STRING, null),
                        new ColumnConfig("num", CsvSchema.ColumnType.NUMBER, null)
                ));
        // When
        var op = new DeserializeJson().create(config, pluginLookup, argumentType);
        // Then
        assertThat(op.inputType()).isEqualTo(ByteBuffer.class);
        assertThat(op.outputType()).isEqualTo(Row.class);
        // And When
        Object parsed = op.apply(ByteBuffer.wrap("\"hello\",1".getBytes(StandardCharsets.UTF_8)), opContext);
        // Then
        assertThat(parsed).asInstanceOf(InstanceOfAssertFactories.type(Row.class))
                .isEqualTo(new Row("hello", 1));
    }
}

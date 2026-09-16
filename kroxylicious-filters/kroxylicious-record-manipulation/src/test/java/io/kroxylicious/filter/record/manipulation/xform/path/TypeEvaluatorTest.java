/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.record.manipulation.xform.path;

import java.lang.reflect.Type;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.JsonNodeFactory;
import tools.jackson.databind.node.MissingNode;
import tools.jackson.databind.node.NumericIntNode;
import tools.jackson.databind.node.StringNode;
import tools.jackson.dataformat.yaml.YAMLMapper;

import static org.assertj.core.api.Assertions.assertThat;

class TypeEvaluatorTest {

    public static final JsonSchemaTypeSystem JSON_SCHEMA_TYPE_SYSTEM = new JsonSchemaTypeSystem(new JsonNodeFactory());
    public static YAMLMapper yamlMapper;
    @BeforeAll
    public static void setup() {
        yamlMapper = new YAMLMapper();
    }
    @AfterAll
    public static void tearDown() {
        yamlMapper = null;
    }

    @Test
    void evaluateObjectPropertiesOpen() {
        var schema = yamlMapper.readTree("""
                type: object
                properties:
                  foo:
                    type: integer
                """);
        TypeEvaluator<JsonNode, JsonNode> te = new TypeEvaluator<>();
        Type foo = te.type(new Path<>(Identifier.ROOT, new Segment.Child<>(new Selector.Name<>("foo")), null), schema, JSON_SCHEMA_TYPE_SYSTEM);
        Type bar = te.type(new Path<>(Identifier.ROOT, new Segment.Child<>(new Selector.Name<>("bar")), null), schema, JSON_SCHEMA_TYPE_SYSTEM);
        assertThat(foo).isEqualTo(NumericIntNode.class);
        assertThat(bar).isEqualTo(JsonNode.class);
    }

    @Test
    void evaluateObjectPropertiesClosed() {
        var schema = yamlMapper.readTree("""
                type: object
                properties:
                  foo:
                    type: integer
                additionalProperties: false
                """);
        TypeEvaluator<JsonNode, JsonNode> te = new TypeEvaluator<>();
        Type foo = te.type(new Path<>(Identifier.ROOT, new Segment.Child<>(new Selector.Name<>("foo")), null), schema, JSON_SCHEMA_TYPE_SYSTEM);
        Type bar = te.type(new Path<>(Identifier.ROOT, new Segment.Child<>(new Selector.Name<>("bar")), null), schema, JSON_SCHEMA_TYPE_SYSTEM);
        assertThat(foo).isEqualTo(NumericIntNode.class);
        assertThat(bar).isEqualTo(MissingNode.class);
    }

    @Test
    void evaluateObjectAdditionalProperties() {
        var schema = yamlMapper.readTree("""
                type: object
                properties:
                  foo:
                    type: integer
                additionalProperties:
                  type: string
                """);
        TypeEvaluator<JsonNode, JsonNode> te = new TypeEvaluator<>();
        Type bar = te.type(new Path<>(Identifier.ROOT, new Segment.Child<>(new Selector.Name<>("bar")), null), schema, JSON_SCHEMA_TYPE_SYSTEM);
        assertThat(bar).isEqualTo(StringNode.class);
    }

    @Test
    void evaluateArrayItems() {
        var schema = yamlMapper.readTree("""
                type: array
                items:
                  type: integer""");
        TypeEvaluator<JsonNode, JsonNode> te = new TypeEvaluator<>();
        Type foo = te.type(new Path<>(Identifier.ROOT, new Segment.Child<>(new Selector.Index<>(0)), null), schema, JSON_SCHEMA_TYPE_SYSTEM);
        Type bar = te.type(new Path<>(Identifier.ROOT, new Segment.Child<>(new Selector.Index<>(1)), null), schema, JSON_SCHEMA_TYPE_SYSTEM);
        assertThat(foo).isEqualTo(NumericIntNode.class);
        assertThat(bar).isEqualTo(NumericIntNode.class);
    }

    @Test
    void evaluateArrayPrefixItems() {
        var schema = yamlMapper.readTree("""
                type: array
                prefixItems:
                  - type: string
                items:
                  type: integer""");
        TypeEvaluator<JsonNode, JsonNode> te = new TypeEvaluator<>();
        Type foo = te.type(new Path<>(Identifier.ROOT, new Segment.Child<>(new Selector.Index<>(0)), null), schema, JSON_SCHEMA_TYPE_SYSTEM);
        Type bar = te.type(new Path<>(Identifier.ROOT, new Segment.Child<>(new Selector.Index<>(1)), null), schema, JSON_SCHEMA_TYPE_SYSTEM);
        assertThat(foo).isEqualTo(StringNode.class);
        assertThat(bar).isEqualTo(NumericIntNode.class);
    }

    @Test
    void evaluateArrayPrefixItemsClosed() {
        YAMLMapper mapper = new YAMLMapper();
        var schema = mapper.readTree("""
                type: array
                prefixItems:
                  - type: string
                items: false""");
        TypeEvaluator<JsonNode, JsonNode> te = new TypeEvaluator<>();
        Type foo = te.type(new Path<>(Identifier.ROOT, new Segment.Child<>(new Selector.Index<>(0)), null), schema, JSON_SCHEMA_TYPE_SYSTEM);
        Type bar = te.type(new Path<>(Identifier.ROOT, new Segment.Child<>(new Selector.Index<>(1)), null), schema, JSON_SCHEMA_TYPE_SYSTEM);
        assertThat(foo).isEqualTo(StringNode.class);
        assertThat(bar).isEqualTo(MissingNode.class);
    }

    // TODO other selectors: Slice, Child, Filter
    // TODO descendant segment
    // TODO paths with multiple segments

}
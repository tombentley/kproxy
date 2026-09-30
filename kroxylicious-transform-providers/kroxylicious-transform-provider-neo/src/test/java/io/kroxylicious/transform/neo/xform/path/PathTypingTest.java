/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.transform.neo.xform.path;

import java.lang.reflect.Type;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import io.kroxylicious.transform.neo.format.jackson3.JsonSchemaTypeSystem;
import io.kroxylicious.transform.neo.type.Bottom;
import io.kroxylicious.transform.neo.type.SchemaType;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.JsonNodeFactory;
import tools.jackson.databind.node.NumericIntNode;
import tools.jackson.databind.node.StringNode;
import tools.jackson.dataformat.yaml.YAMLMapper;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Exercises {@link TypeCheckingVisitor#typeOfPath} against the {@link JsonSchemaTypeSystem}, verifying that path
 * typing remains type-system-agnostic now that it is built into the visitor rather than a standalone evaluator.
 */
class PathTypingTest {

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

    private static Type typeOf(Ast.Path<JsonNode> path, JsonNode schema) {
        return TypeCheckingVisitor.typeOfPath(path, SchemaType.of(JSON_SCHEMA_TYPE_SYSTEM, schema));
    }

    @Test
    void evaluateObjectPropertiesOpen() {
        var schema = yamlMapper.readTree("""
                type: object
                properties:
                  foo:
                    type: integer
                """);
        Type foo = typeOf(new Ast.Path<>(Identifier.ROOT, new Ast.Segment.Child<>(new Ast.Selector.Name<>("foo")), null), schema);
        Type bar = typeOf(new Ast.Path<>(Identifier.ROOT, new Ast.Segment.Child<>(new Ast.Selector.Name<>("bar")), null), schema);
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
        Type foo = typeOf(new Ast.Path<>(Identifier.ROOT, new Ast.Segment.Child<>(new Ast.Selector.Name<>("foo")), null), schema);
        Type bar = typeOf(new Ast.Path<>(Identifier.ROOT, new Ast.Segment.Child<>(new Ast.Selector.Name<>("bar")), null), schema);
        assertThat(foo).isEqualTo(NumericIntNode.class);
        // a property that a closed object cannot have is an impossible navigation: the uninhabited bottom type
        assertThat(bar).isEqualTo(Bottom.class);
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
        Type bar = typeOf(new Ast.Path<>(Identifier.ROOT, new Ast.Segment.Child<>(new Ast.Selector.Name<>("bar")), null), schema);
        assertThat(bar).isEqualTo(StringNode.class);
    }

    @Test
    void evaluateArrayItems() {
        var schema = yamlMapper.readTree("""
                type: array
                items:
                  type: integer""");
        Type foo = typeOf(new Ast.Path<>(Identifier.ROOT, new Ast.Segment.Child<>(new Ast.Selector.Index<>(0)), null), schema);
        Type bar = typeOf(new Ast.Path<>(Identifier.ROOT, new Ast.Segment.Child<>(new Ast.Selector.Index<>(1)), null), schema);
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
        Type foo = typeOf(new Ast.Path<>(Identifier.ROOT, new Ast.Segment.Child<>(new Ast.Selector.Index<>(0)), null), schema);
        Type bar = typeOf(new Ast.Path<>(Identifier.ROOT, new Ast.Segment.Child<>(new Ast.Selector.Index<>(1)), null), schema);
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
        Type foo = typeOf(new Ast.Path<>(Identifier.ROOT, new Ast.Segment.Child<>(new Ast.Selector.Index<>(0)), null), schema);
        Type bar = typeOf(new Ast.Path<>(Identifier.ROOT, new Ast.Segment.Child<>(new Ast.Selector.Index<>(1)), null), schema);
        assertThat(foo).isEqualTo(StringNode.class);
        // an index a closed array cannot have is an impossible navigation: the uninhabited bottom type
        assertThat(bar).isEqualTo(Bottom.class);
    }

    // TODO other selectors: Slice, Child, Filter
    // TODO descendant segment
    // TODO paths with multiple segments

}

/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.record.manipulation.xform.path;

import java.util.List;

import org.junit.jupiter.api.Test;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.JsonNodeFactory;
import tools.jackson.dataformat.yaml.YAMLMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

class JsonSchemaTypeSystemTest {

    public static final JsonSchemaTypeSystem JSON_SCHEMA_TYPE_SYSTEM = new JsonSchemaTypeSystem(new JsonNodeFactory());

    @Test
    void propertiesNoAdditionalProperties() {
        YAMLMapper mapper = new YAMLMapper();
        var schema = mapper.readTree("""
                type: object
                properties:
                  foo:
                    type: integer
                  bar:
                    type: string""");
        JsonNode foo = JSON_SCHEMA_TYPE_SYSTEM.objectPropertySchema(schema, "foo");
        JsonNode bar = JSON_SCHEMA_TYPE_SYSTEM.objectPropertySchema(schema, "bar");
        JsonNode baz = JSON_SCHEMA_TYPE_SYSTEM.objectPropertySchema(schema, "baz");
        assertThat(JSON_SCHEMA_TYPE_SYSTEM.objectProperties(schema)).isEqualTo(List.of("foo", "bar"));
        assertThat(foo.get("type").asString()).isEqualTo("integer");
        assertThat(bar.get("type").asString()).isEqualTo("string");
        assertThat(baz.get("type").toPrettyString())
                .as("additionalProperties defaults to true, and so could have any type")
                .isEqualTo("[ \"null\", \"boolean\", \"number\", \"integer\", \"string\", \"array\", \"object\" ]");
        assertThat(JSON_SCHEMA_TYPE_SYSTEM.isObjectType(schema)).isTrue();
        assertThat(JSON_SCHEMA_TYPE_SYSTEM.isArrayType(schema)).isFalse();
        assertThat(JSON_SCHEMA_TYPE_SYSTEM.arrayIndexes(schema)).isNull();
    }

    @Test
    void propertiesAdditionalPropertiesFalse() {
        YAMLMapper mapper = new YAMLMapper();
        var schema = mapper.readTree("""
                type: object
                properties:
                  foo:
                    type: integer
                  bar:
                    type: string
                additionalProperties: false""");
        JsonNode foo = JSON_SCHEMA_TYPE_SYSTEM.objectPropertySchema(schema, "foo");
        JsonNode bar = JSON_SCHEMA_TYPE_SYSTEM.objectPropertySchema(schema, "bar");
        JsonNode baz = JSON_SCHEMA_TYPE_SYSTEM.objectPropertySchema(schema, "baz");
        assertThat(JSON_SCHEMA_TYPE_SYSTEM.objectProperties(schema)).isEqualTo(List.of("foo", "bar"));
        assertThat(foo.get("type").asString()).isEqualTo("integer");
        assertThat(bar.get("type").asString()).isEqualTo("string");
        assertThat(baz.get("type")).isNull();
        assertThat(JSON_SCHEMA_TYPE_SYSTEM.isObjectType(schema)).isTrue();
        assertThat(JSON_SCHEMA_TYPE_SYSTEM.isArrayType(schema)).isFalse();
        assertThat(JSON_SCHEMA_TYPE_SYSTEM.arrayIndexes(schema)).isNull();
    }

    @Test
    void additionalProperties() {
        YAMLMapper mapper = new YAMLMapper();
        var schema = mapper.readTree("""
                type: object
                properties:
                  foo:
                    type: integer
                additionalProperties:
                  type: string""");
        JsonNode foo = JSON_SCHEMA_TYPE_SYSTEM.objectPropertySchema(schema, "foo");
        JsonNode bar = JSON_SCHEMA_TYPE_SYSTEM.objectPropertySchema(schema, "bar");
        JsonNode bar2 = JSON_SCHEMA_TYPE_SYSTEM.objectPropertySchema(schema);
        assertThat(JSON_SCHEMA_TYPE_SYSTEM.objectProperties(schema)).isEqualTo(List.of("foo"));
        assertThat(foo.get("type").asString()).isEqualTo("integer");
        assertThat(bar.get("type").asString()).isEqualTo("string");
        assertThat(bar).isEqualTo(bar2);
        assertThat(JSON_SCHEMA_TYPE_SYSTEM.isObjectType(schema)).isTrue();
        assertThat(JSON_SCHEMA_TYPE_SYSTEM.isArrayType(schema)).isFalse();
        assertThat(JSON_SCHEMA_TYPE_SYSTEM.arrayIndexes(schema)).isNull();
    }

    @Test
    void items() {
        YAMLMapper mapper = new YAMLMapper();
        var schema = mapper.readTree("""
                type: array
                items:
                  type: integer
                """);
        JsonNode foo = JSON_SCHEMA_TYPE_SYSTEM.arrayItemSchema(schema, 0);
        assertThat(JSON_SCHEMA_TYPE_SYSTEM.arrayIndexes(schema)).isEmpty();
        assertThat(foo.get("type").asString()).isEqualTo("integer");
        assertThat(JSON_SCHEMA_TYPE_SYSTEM.isObjectType(schema)).isFalse();
        assertThat(JSON_SCHEMA_TYPE_SYSTEM.isArrayType(schema)).isTrue();
        assertThat(JSON_SCHEMA_TYPE_SYSTEM.objectProperties(schema)).isEmpty();
    }

}
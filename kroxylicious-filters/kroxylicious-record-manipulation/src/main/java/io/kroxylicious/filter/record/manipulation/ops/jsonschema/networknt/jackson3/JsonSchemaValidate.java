/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.record.manipulation.ops.jsonschema.networknt.jackson3;

import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.networknt.schema.Error;
import com.networknt.schema.InputFormat;
import com.networknt.schema.Schema;
import com.networknt.schema.SchemaException;
import com.networknt.schema.SchemaLocation;
import com.networknt.schema.SchemaRegistry;
import com.networknt.schema.SchemaRegistryConfig;
import com.networknt.schema.SpecificationVersion;
import com.networknt.schema.dialect.Dialects;
import com.networknt.schema.regex.JoniRegularExpressionFactory;

import io.kroxylicious.filter.record.manipulation.op.BaseTypedOp;
import io.kroxylicious.filter.record.manipulation.op.OpFactory;
import io.kroxylicious.filter.record.manipulation.op.PluginLookup;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

public class JsonSchemaValidate implements OpFactory<JsonNode, JsonNode> {
    /**
     * Validate the constructor arguments
     * @param jsonSchema The schema text
     * @param defaultDialect The default dialect used when $schema is not specified in the {@link jsonSchema}, identified by its name like {@code DRAFT_2020_12}
     * @param defaultDialectId The  used when $schema is not specified in the {@link jsonSchema}, identified by an IRI (such as {@code https://json-schema.org/draft/2020-12/schema}).
     */
    public record Config(@JsonProperty(required = true) String jsonSchema,
                         SpecificationVersion defaultDialect,
                         String defaultDialectId) {
        public Config {
            if (defaultDialect != null && defaultDialectId != null) {
                throw new IllegalArgumentException("defaultDialect and defaultDialectId cannot both be specified");
            }
        }
    }
    @Override
    public BaseTypedOp<JsonNode, JsonNode> create(Map<String, Object> configMap, PluginLookup lookup, Type argumentType) {
        ObjectMapper objectMapper = new ObjectMapper();
        Config config = objectMapper.convertValue(configMap, Config.class);
        /*
         * The SchemaRegistryConfig can be optionally used to configure certain aspects
         * of how the validation is performed.
         *
         * By default the JDK regular expression implementation which is not ECMA 262
         * compliant is used. The GraalJSRegularExpressionFactory.getInstance() offers
         * the best compliance followed by JoniRegularExpressionFactory.getInstance()
         * but both require additional optional dependencies.
         */
        SchemaRegistryConfig schemaRegistryConfig = SchemaRegistryConfig.builder()
                .regularExpressionFactory(JoniRegularExpressionFactory.getInstance()).build();

        /*
         * This creates a schema registry that supports all the standard dialects for
         * cross-dialect validation and will use Draft 2020-12 as the default if $schema
         * is not specified in the schema data. If $schema is specified in the schema
         * data then that schema dialect will be used instead and this version is
         * ignored.
         */

        SchemaRegistry schemaRegistry = SchemaRegistry.withDefaultDialect(SpecificationVersion.fromDialectId(config.defaultDialectId()).orElse(
                SpecificationVersion.DRAFT_2020_12
        ));

        JsonNode schemaNode = parseSchema(objectMapper, config);
        validateSchema(schemaRegistry, schemaNode);
        Schema schema = getSchema(schemaRegistry, schemaNode);
        return BaseTypedOp.<JsonNode, JsonNode>of(JsonNode.class, JsonNode.class,
                (value, context) -> validate(value, schema));
    }

    private static Schema getSchema(SchemaRegistry schemaRegistry, JsonNode schemaNode) {
        Schema schema;
        try {
            schema = schemaRegistry.getSchema(schemaNode);
        } catch (SchemaException e) {
            throw new SchemaNotValidException("Given value for 'jsonSchema' was not a valid JsonSchema", e);
        } catch (Exception e) {
            throw new SchemaNotValidException("Given value for 'jsonSchema' was not accepted", e);
        }
        return schema;
    }

    private static void validateSchema(SchemaRegistry schemaRegistry, JsonNode schemaNode) {
        Schema metaschema = schemaRegistry.getSchema(SchemaLocation.of(Dialects.getDraft202012().getId()));
        List<Error> schemaErrors = metaschema.validate(schemaNode, executionContext -> {
            /*
             * By default since Draft 2019-09 the format keyword only generates annotations
             * and not assertions.
             */
            executionContext.executionConfig(executionConfig -> executionConfig.formatAssertionsEnabled(true));
        });
        if (!schemaErrors.isEmpty()) {
            throw new SchemaNotValidException("Given value for 'jsonSchema' was not a valid JsonSchema: " + schemaErrors.getFirst());
        }
    }

    private static JsonNode parseSchema(ObjectMapper objectMapper, Config config) {
        JsonNode schemaNode;
        try {
            schemaNode = objectMapper.readTree(config.jsonSchema());
        } catch (JacksonException e) {
            throw new SchemaNotValidException("Given value for 'jsonSchema' was not valid JSON", e);
        }
        return schemaNode;
    }

    private static JsonNode validate(JsonNode value, Schema schema) {
        List<Error> errors = schema.validate(value, executionContext -> {
            executionContext.executionConfig(executionConfig -> executionConfig.formatAssertionsEnabled(true));
        });
        if (errors.isEmpty()) {
            return value;
        }
        throw new DataNotSchemaValidException(errors.toString());
    }
}

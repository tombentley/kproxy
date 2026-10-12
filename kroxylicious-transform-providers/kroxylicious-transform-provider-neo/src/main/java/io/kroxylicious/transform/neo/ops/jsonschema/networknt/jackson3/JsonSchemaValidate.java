/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.transform.neo.ops.jsonschema.networknt.jackson3;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.reflect.Type;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.networknt.schema.Error;
import com.networknt.schema.Schema;
import com.networknt.schema.SchemaException;
import com.networknt.schema.SchemaLocation;
import com.networknt.schema.SchemaRegistry;
import com.networknt.schema.SchemaRegistryConfig;
import com.networknt.schema.SpecificationVersion;
import com.networknt.schema.dialect.Dialects;
import com.networknt.schema.regex.JoniRegularExpressionFactory;

import io.kroxylicious.filter.record.manipulation.filter.apicurio.api.GavCoordinates;
import io.kroxylicious.filter.record.manipulation.filter.apicurio.api.GlobalId;
import io.kroxylicious.filter.record.manipulation.filter.apicurio.api.SchemaRegistryClient;
import io.kroxylicious.proxy.plugin.Plugin;
import io.kroxylicious.transform.neo.format.jackson3.JsonSchemaTypeSystem;
import io.kroxylicious.transform.neo.op.BaseTypedOp;
import io.kroxylicious.transform.neo.op.OpFactory;
import io.kroxylicious.transform.neo.type.SchemaType;
import io.kroxylicious.transform.service.TransformInitializationContext;

import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.Nullable;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.JsonNodeFactory;

/**
 * <pre>{@code
 *   schema:
 *     fromValue: |
 *       <your schema>
 *   defaultDialect: DRAFT_2020_12
 * }</pre>
 *
 * <pre>{@code
 *   schema:
 *     fromFile: /path/to/your/schema
 *   defaultDialectId: https://json-schema.org/draft/2020-12/schema
 * }</pre>
 *
 * <pre>{@code
 *   schema:
 *     fromRegistry:
 *       groupId: my-group
 *       artifactId: com.example.my.app.User
 *   defaultDialectId: https://json-schema.org/draft/2020-12/schema
 * }</pre>
 *
 * <pre>{@code
 *   schema:
 *     fromDatum: SCHEMA_ID_PREFIX
 *   defaultDialectId: https://json-schema.org/draft/2020-12/schema
 * }</pre>
 *
 */
@Plugin(configType = JsonSchemaValidate.Config.class)
public class JsonSchemaValidate implements OpFactory<JsonNode, JsonNode> {
    public enum DatumStrategy {
        /** Magic byte plus 4 byte schema id */
        SCHEMA_ID_PREFIX
        // /** Magic byte plus 8 byte schema id */
        //SCHEMA_ID_LONG_PREFIX
        // /** Apicurio header */
        //SCHEMA_ID_FROM_HEADER
    }
    public record SchemaReference(
            @Nullable String value,
            @Nullable Path fromFile,
            @Nullable RegistrySchemaReference fromRegistry,
            @Nullable DatumStrategy fromDatum) {
        public SchemaReference {
            if (value != null) {
                if (fromFile != null ||  fromRegistry != null) {
                    throw new IllegalArgumentException("Exactly one of 'value', 'fromFile', or 'fromRegistry' must be set");
                }
            }
            else if (fromFile != null) {
                if (fromRegistry != null) {
                    throw new IllegalArgumentException("Exactly one of 'value', 'fromFile', or 'fromRegistry' must be set");
                }
            }
            else if (fromRegistry == null) {
                throw new IllegalArgumentException("Exactly one of 'value', 'fromFile', or 'fromRegistry' must be set");
            }
        }
    }

    public record RegistrySchemaReference(
                                          @JsonProperty(required = true) String groupId,
                                          @JsonProperty(required = true) String artifactId,
                                          @Nullable String version) {
        public RegistrySchemaReference(@JsonProperty(required = true) String groupId, @JsonProperty(required = true) String artifactId, @Nullable String version) {
            this.groupId = groupId;
            this.artifactId = artifactId;
            this.version = version != null ? version : "LATEST";
        }
    }

    /**
     * Validate the constructor arguments
     * @param jsonSchema The schema
     * @param defaultDialect The default dialect used when $withKeySchema is not specified in the {@link jsonSchema}, identified by its name like {@code DRAFT_2020_12}
     * @param defaultDialectId The  used when $withKeySchema is not specified in the {@link jsonSchema}, identified by an IRI (such as {@code https://json-schema.org/draft/2020-12/schema}).
     */
    public record Config(@JsonProperty(required = true) SchemaReference jsonSchema,
                         @Nullable SpecificationVersion defaultDialect,
                         @Nullable String defaultDialectId) {
        public Config {
            if (defaultDialect != null && defaultDialectId != null) {
                throw new IllegalArgumentException("defaultDialect and defaultDialectId cannot both be specified");
            }
        }
    }

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public BaseTypedOp<JsonNode, JsonNode> create(Map<String, Object> configMap,
                                                  TransformInitializationContext transformInitializationContext,
                                                  Type argumentType) {

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
         * This creates a withKeySchema registry that supports all the standard dialects for
         * cross-dialect validation and will use Draft 2020-12 as the default if $withKeySchema
         * is not specified in the withKeySchema data. If $withKeySchema is specified in the withKeySchema
         * data then that withKeySchema dialect will be used instead and this version is
         * ignored.
         */

        SchemaRegistry schemaRegistry = SchemaRegistry.withDefaultDialect(
                SpecificationVersion.fromDialectId(config.defaultDialectId())
                        .orElse(SpecificationVersion.DRAFT_2020_12));


        SchemaReference schemaReference = config.jsonSchema();
        if (schemaReference.value() != null) {
            return createSyncValidator(schemaRegistry, schemaReference.value());
        } else if (schemaReference.fromFile() != null) {
            try {
                return createSyncValidator(schemaRegistry, Files.readString(schemaReference.fromFile()));
            }
            catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        } else {
            return createAsyncValidator(transformInitializationContext, schemaReference, schemaRegistry);
        }
    }

    @NonNull
    private BaseTypedOp<JsonNode, JsonNode> createAsyncValidator(TransformInitializationContext transformInitializationContext,
                                                                 SchemaReference schemaReference,
                                                                 SchemaRegistry schemaRegistry) {
        SchemaRegistryClient sr = transformInitializationContext.serviceClient(SchemaRegistryClient.class, "");
        if (schemaReference.fromDatum() != null) {
            return BaseTypedOp.<ByteBuffer, JsonNode> ofAsync(ByteBuffer.class, JsonNode.class,
                    (value, context) -> {
                        if (value.get(0) != 0x00) {
                            throw new ReferenceNotValidException("");
                        }
                        int anInt = value.getInt(1);
                        return sr.versionsForGlobalId(new GlobalId(anInt))
                                .thenApply(versionsResult -> {
                                    return sr.contentById(versionsResult.contentId())
                                            .thenApply(contentResult -> {
                                                if (!"JSON".equals(contentResult.type())) {
                                                    throw new SchemaNotValidException("Configured schema coordinates " + schemaReference.fromRegistry()
                                                            + " identify content of type " + contentResult.type()
                                                            + " rather than JsonSchema content, as required");
                                                }
                                                return parseSchemaAndValidate(schemaRegistry, new String(contentResult.contentBytes(), StandardCharsets.UTF_8), value);
                                            });
                                });
                    });
        } else if (schemaReference.fromRegistry() != null) {

            return BaseTypedOp.<JsonNode, JsonNode> ofAsync(JsonNode.class, JsonNode.class,
                    (value, context) -> {
                        // TODO problem: While SR might cache the raw schema bytes, we're still having to parse the schema on
                        //   every invocation.
                        //   We can cache the schema (it's thread-safe), but then the byte level caching is not a good use of resources
                        return sr.contentByGav(new GavCoordinates(schemaReference.fromRegistry.groupId(),
                                        schemaReference.fromRegistry.artifactId(), schemaReference.fromRegistry.version()))
                                .thenApply(contentResult -> {
                                    if (!"JSON".equals(contentResult.type())) {
                                        throw new SchemaNotValidException("Configured schema coordinates " + schemaReference.fromRegistry()
                                                + " identify content of type " + contentResult.type()
                                                + " rather than JsonSchema content, as required");
                                    }
                                    return parseSchemaAndValidate(schemaRegistry, new String(contentResult.contentBytes(), StandardCharsets.UTF_8), value);
                                });
                    },
                    null);

        } else {
            throw new IllegalStateException();
        }
    }

    @NonNull
    private JsonNode parseSchemaAndValidate(SchemaRegistry schemaRegistry, String schemaString, JsonNode value) {

        // TODO cache the schema
        JsonNode schemaNode = parseSchemaFromString(objectMapper, schemaString);
        validateSchema(schemaRegistry, schemaNode);
        Schema schema = lookupSchema(schemaRegistry, schemaNode);
        return validate(value, schema);
    }

    private BaseTypedOp<JsonNode, JsonNode> createSyncValidator(SchemaRegistry schemaRegistry, String schemaString) {
        JsonNode schemaNode = parseSchemaFromString(objectMapper, schemaString);
        validateSchema(schemaRegistry, schemaNode);
        Schema schema = lookupSchema(schemaRegistry, schemaNode);
        return newValidationOp(schema, schemaNode);
    }

    @NonNull
    private static BaseTypedOp<JsonNode, JsonNode> newValidationOp(Schema schema, JsonNode schemaNode) {
        return BaseTypedOp.<JsonNode, JsonNode> of(JsonNode.class, JsonNode.class,
                (value, context) -> validate(value, schema),
                SchemaType.of(new JsonSchemaTypeSystem(new JsonNodeFactory()), schemaNode));
    }

    private static Schema lookupSchema(SchemaRegistry schemaRegistry, JsonNode schemaNode) {
        Schema schema;
        try {
            schema = schemaRegistry.getSchema(schemaNode);
        }
        catch (SchemaException e) {
            throw new SchemaNotValidException("Given value for 'jsonSchema' was not a valid JsonSchema", e);
        }
        catch (Exception e) {
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

    private static JsonNode parseSchemaFromString(ObjectMapper objectMapper, String jsonSchema) {
        JsonNode schemaNode;
        try {
            schemaNode = objectMapper.readTree(jsonSchema);
        }
        catch (JacksonException e) {
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

/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.schema;

import com.networknt.schema.InputFormat;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SchemaId;
import com.networknt.schema.SchemaLocation;
import com.networknt.schema.SchemaValidatorsConfig;
import com.networknt.schema.SpecVersion;
import com.networknt.schema.ValidationMessage;
import com.networknt.schema.ValidationResult;
import com.networknt.schema.annotation.JsonNodeAnnotation;
import com.networknt.schema.walk.JsonSchemaWalkListener;
import com.networknt.schema.walk.WalkEvent;
import com.networknt.schema.walk.WalkFlow;

import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;
import java.util.Set;

public class Main {




    // Our additional restrictions:
    // We add x-kubernetes-validations because we don't want to support CEL right now

    private static final JsonSchemaWalkListener propertyWalkListener = new JsonSchemaWalkListener() {
        @Override
        public WalkFlow onWalkStart(WalkEvent walkEvent) {
                        walkEvent.getExecutionContext().getCollectorContext().add("foo", "bar");
                        walkEvent.getExecutionContext().getAnnotations().put(new JsonNodeAnnotation("gee",
                                walkEvent.getInstanceLocation(),
                                walkEvent.getSchema().getSchemaLocation(),
                                null,
                                "wwww"));
                        walkEvent.getExecutionContext().getResults().setResult(walkEvent.getInstanceLocation(),
                                walkEvent.getSchema().getSchemaLocation(), walkEvent.getInstanceLocation(), false);
            System.out.println(walkEvent.getKeyword());
            System.out.println(walkEvent.getSchema().getSchemaNode().get(walkEvent.getKeyword()));
            System.out.println(walkEvent);
            return WalkFlow.CONTINUE;
        }

        @Override
        public void onWalkEnd(
                WalkEvent walkEvent,
                Set<ValidationMessage> validationMessages
        ) {
            System.out.println("/" + walkEvent);
        }
    };

    public static void main(String[] a) throws IOException {
        SchemaValidatorsConfig.Builder builder = SchemaValidatorsConfig.builder();

        //builder.propertyWalkListener(propertyWalkListener);
        builder.keywordWalkListener("definitions", propertyWalkListener); // good
        builder.keywordWalkListener("oneOf", propertyWalkListener); // good
        builder.keywordWalkListener("items", propertyWalkListener); // good

        //builder.itemWalkListener(propertyWalkListener);
        //builder.strict(ValidatorTypeCode.PROPERTIES.getValue(), true);
        SchemaValidatorsConfig config = builder.build();

        //JsonSchemaFactory jsonSchemaFactory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V4);
        JsonSchemaFactory jsonSchemaFactory = JsonSchemaFactory.builder(JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V4))
                //.metaSchema(null)
                .schemaMappers(
                        schemaMappers -> schemaMappers.mapPrefix("https://www.example.org/", "classpath:schema/")
                )
                .build();
        // Due to the mapping the meta-schema will be retrieved from the classpath at classpath:draft/2020-12/schema.
        JsonSchema metaschema = jsonSchemaFactory.getSchema(SchemaLocation.of(SchemaId.V4), config);

        //        String input = "{\r\n"
        //                + "  \"type\": \"object\",\r\n"
        //                + "  \"properties\": {\r\n"
        //                + "    \"key\": {\r\n"
        //                + "      \"title\" : \"My key\",\r\n"
        //                + "      \"type\": \"invalidtype\"\r\n"
        //                + "    }\r\n"
        //                + "  }\r\n"
        //                + "}";
        String schemaAsString;
        try (InputStream resourceAsStream = Objects.requireNonNull(KroxySchemaValidator.class.getResourceAsStream("/schema/config-schema.yaml"))) {
            schemaAsString = new String(resourceAsStream.readAllBytes());
        }

        Set<ValidationMessage> schemaAssertions = metaschema.validate(schemaAsString, InputFormat.YAML, executionContext -> {
            // By default since Draft 2019-09 the format keyword only generates annotations and not assertions
            executionContext.getExecutionConfig().setFormatAssertionsEnabled(true);
        });
        System.out.println("000: " + schemaAssertions);

        JsonSchema schema = jsonSchemaFactory.getSchema(schemaAsString, InputFormat.YAML, config);
        System.out.println("001");
        ValidationResult walk = schema.walk(schema.createExecutionContext(), schemaAsString, InputFormat.YAML, true);
        System.out.println("456: " + walk.getValidationMessages());
        System.out.println("789: " + walk.getCollectorContext().getCollectorMap());

        String instanceAsString = """
                {
                  "virtualClusters": {
                    "common": {
                      "targetCluster": {
                        
                      },
                      "clusterNetworkAddressConfigProvider": {
                        "type": "1234"
                      }
                    }
                  }
                }
                """;



        Set<ValidationMessage> instanceAssertions = schema.validate(instanceAsString, InputFormat.JSON, executionContext -> {
            // By default since Draft 2019-09 the format keyword only generates annotations and not assertions
            executionContext.getExecutionConfig().setFormatAssertionsEnabled(true);
        });

        System.out.println("123: " + instanceAssertions);

        //validateConfig();
    }

    private static void validateConfig() {
        // This creates a schema factory that will use Draft 2020-12 as the default if $schema is not specified
        // in the schema data. If $schema is specified in the schema data then that schema dialect will be used
        // instead and this version is ignored.
        JsonSchemaFactory jsonSchemaFactory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V4, builder ->
                // This creates a mapping from $id which starts with https://www.example.org/ to the retrieval URI classpath:schema/
                builder.schemaMappers(schemaMappers -> schemaMappers.mapPrefix("https://www.example.org/", "classpath:schema/"))
        );

        SchemaValidatorsConfig.Builder builder = SchemaValidatorsConfig.builder();
        // By default the JDK regular expression implementation which is not ECMA 262 compliant is used
        // Note that setting this requires including optional dependencies
        // builder.regularExpressionFactory(GraalJSRegularExpressionFactory.getInstance());
        // builder.regularExpressionFactory(JoniRegularExpressionFactory.getInstance());
        SchemaValidatorsConfig config = builder.build();

        // Due to the mapping the schema will be retrieved from the classpath at classpath:schema/example-main.json.
        // If the schema data does not specify an $id the absolute IRI of the schema location will be used as the $id.
        JsonSchema schema = jsonSchemaFactory.getSchema(SchemaLocation.of("https://www.example.org/config-schema.yaml"), config);
        String input = """
                {
                  "virtualClusters": {
                    "common": {
                      "clusterNetworkAddressConfigProvider": {
                        "type": "1234",
                        "config": {}
                      }
                    }
                  }
                }
                """;

        Set<ValidationMessage> assertions = schema.validate(input, InputFormat.JSON, executionContext -> {
            // By default since Draft 2019-09 the format keyword only generates annotations and not assertions
            executionContext.getExecutionConfig().setFormatAssertionsEnabled(true);
        });

        System.out.println(assertions);
    }
}
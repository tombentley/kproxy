/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.stream.Collector;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SchemaId;
import com.networknt.schema.SchemaLocation;
import com.networknt.schema.SchemaValidatorsConfig;
import com.networknt.schema.SpecVersion;
import com.networknt.schema.ValidationMessage;
import com.networknt.schema.ValidationResult;
import com.networknt.schema.walk.JsonSchemaWalkListener;

public class ConfigValidator {

    public static final String DEFINITIONS = "definitions";
    public static final String ADDITIONAL_PROPERTIES = "additionalProperties";
    public static final String ITEMS = "items";
    public static final String ADDITIONAL_ITEMS = "additionalItems";
    public static final String NOT = "not";
    public static final String PROPERTIES = "properties";
    public static final String PATTERN_PROPERTIES = "patternProperties";
    public static final String DEPENDENCIES = "dependencies";
    public static final String ALL_OF = "allOf";
    public static final String ONE_OF = "oneOf";
    public static final String ANY_OF = "anyOf";
    public static final String DEPRECATED = "deprecated";
    public static final String DISCRIMINATOR = "discriminator";
    public static final String ID = "id";
    public static final String READ_ONLY = "readOnly";
    public static final String WRITE_ONLY = "writeOnly";
    public static final String XML = "xml";
    public static final String REF = "$ref";

    private final YAMLMapper mapper;
    private JsonNode root;

    public ConfigValidator() {
        mapper = new YAMLMapper();
    }

    public void doIt() throws IOException {
        this.parse();
        // Resolve definitions before doing the networknt validation
        // because its visitor/walker mechanism does not visit keywords in referenced schemas
        // meaning
        this.resolveDefinitions();
        this.validateAgainstDraft4();

        // resolveDefinitions must run before checkKubeConstraints because
        // we want to allow authors to use local $refs
        this.checkKubeConstraints();
        this.checkIsStructural();

    }

    /**
     * Replaces {@code $ref} iff they're local to the scale (in some {@code definitions}).
     * Rejects the schema if there are any {@code $ref} that point externally
     */
    private void parse() throws IOException {
        String schemaAsString;
        try (InputStream resourceAsStream = Objects.requireNonNull(ConfigValidator.class.getResourceAsStream("/schema/config-schema.yaml"))) {
            schemaAsString = new String(resourceAsStream.readAllBytes());
        }
        root = mapper.readTree(schemaAsString);
    }

    /**
     * Replaces {@code $ref} iff they're local to the scale (in some {@code definitions}).
     * Rejects the schema if there are any {@code $ref} that point externally
     */
    private void validateAgainstDraft4() {
        // TODO Require draft 4
        // TODO And no unknown keywords
        SchemaValidatorsConfig.Builder builder = SchemaValidatorsConfig.builder();
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
        Set<ValidationMessage> schemaAssertions = metaschema.validate(root, executionContext -> {
            // By default since Draft 2019-09 the format keyword only generates annotations and not assertions
            executionContext.getExecutionConfig().setFormatAssertionsEnabled(true);
        });
    }

    static class KeywordVisitor {
        void visitType(ObjectNode schema) { }
        void visitProperties(ObjectNode object) { }
        void visitDefinition(ObjectNode definitions) { }
    }

    /**
     * Replaces {@code $ref} iff they're local to the scale (in some {@code definitions}).
     * Rejects the schema if there are any {@code $ref} that point externally
     */
    private void resolveDefinitions() {
        var byId = resolveDefinitionsRecursive("#", root);
        var byId2 = visitSchemas("#", root, Collectors.toMap(e -> e.getKey(), e -> e.getValue()),
                Map::entry);
        // TODO use the map to replace nodes
    }

    private static Map<String, JsonNode> resolveDefinitionsRecursive(
            String base,
            JsonNode schema
    ) {
        Map<String, JsonNode> byId = new HashMap<>();
        var defs = schema.get(DEFINITIONS);
        if (!defs.isObject()) {
            throw new IllegalStateException(DEFINITIONS + " must be an object in Draft 4");
        }
        for (var fieldIterator = defs.fields();
                fieldIterator.hasNext() ;
        ) {
            var fieldEntry = fieldIterator.next();
            var fieldName = fieldEntry.getKey();
            String id = base + "/" + DEFINITIONS + "/" + fieldName;
            byId.put(id, fieldEntry.getValue());
        }

        // recurse into all the subschemas
        for (String keyword : List.of(
                ADDITIONAL_PROPERTIES,
                ITEMS,
                ADDITIONAL_ITEMS,
                NOT)) {
            // these keywords all support values that are schemas
            var subschema = schema.get(keyword);
            if (subschema.isObject()) {
                byId.putAll(resolveDefinitionsRecursive(
                        base + "/" + keyword + "/",
                        subschema));
            }
        }
        for (String keyword : List.of(
                PROPERTIES,
                PATTERN_PROPERTIES,
                DEPENDENCIES,
                DEFINITIONS)) {
            // these keywords all support values that are objects with values that are schemas
            var obj = schema.get(keyword);
            if (obj.isObject()) {
                for (var subschemaIterator = obj.fields() ; subschemaIterator.hasNext() ; ) {
                    var subschemaEntry = subschemaIterator.next();
                    byId.putAll(resolveDefinitionsRecursive(
                            base + "/" + keyword + "/" + subschemaEntry.getKey() + "/",
                            subschemaEntry.getValue()));
                }
            }
        }
        for (String keyword : List.of(
                ITEMS,
                ALL_OF,
                ONE_OF,
                ANY_OF)) {
            // these keywords all support values that are arrays with items that are schemas
            var arr = schema.get(keyword);
            if (arr.isArray()) {
                int index = 0;
                for (var subschema : arr) {
                    byId.putAll(resolveDefinitionsRecursive(
                            base + "/" + keyword + "/" + index + "/",
                            subschema));
                    index++;
                }
            }
        }
        return byId;
    }

    private static <T, A, R> A visitSchemas(
            String base,
            JsonNode schema,
            Collector<T, A, R> collector,
            BiFunction<String, JsonNode, T> fn
    ) {
        A byId = collector.supplier().get();
        var defs = schema.get(DEFINITIONS);
        if (!defs.isObject()) {
            throw new IllegalStateException(DEFINITIONS + " must be an object in Draft 4");
        }
        for (var fieldIterator = defs.fields();
                fieldIterator.hasNext() ;
        ) {
            var fieldEntry = fieldIterator.next();
            var fieldName = fieldEntry.getKey();
            String id = base + "/" + DEFINITIONS + "/" + fieldName;
            collector.accumulator().accept(byId, fn.apply(id, fieldEntry.getValue()));
        }

        // recurse into all the subschemas
        for (String keyword : List.of(
                ADDITIONAL_PROPERTIES,
                ITEMS,
                ADDITIONAL_ITEMS,
                NOT)) {
            // these keywords all support values that are schemas
            var subschema = schema.get(keyword);
            if (subschema.isObject()) {
                collector.combiner().apply(byId, visitSchemas(
                        base + "/" + keyword + "/",
                        subschema,
                        collector,
                        fn));
            }
        }
        for (String keyword : List.of(
                PROPERTIES,
                PATTERN_PROPERTIES,
                DEPENDENCIES,
                DEFINITIONS)) {
            // these keywords all support values that are objects with values that are schemas
            var obj = schema.get(keyword);
            if (obj.isObject()) {
                for (var subschemaIterator = obj.fields() ; subschemaIterator.hasNext() ; ) {
                    var subschemaEntry = subschemaIterator.next();
                    collector.combiner().apply(byId, visitSchemas(
                            base + "/" + keyword + "/" + subschemaEntry.getKey() + "/",
                            subschemaEntry.getValue(),
                            collector,
                            fn));
                }
            }
        }
        for (String keyword : List.of(
                ITEMS,
                ALL_OF,
                ONE_OF,
                ANY_OF)) {
            // these keywords all support values that are arrays with items that are schemas
            var arr = schema.get(keyword);
            if (arr.isArray()) {
                int index = 0;
                for (var subschema : arr) {
                    collector.combiner().apply(byId, visitSchemas(
                            base + "/" + keyword + "/" + index + "/",
                            subschema,
                            collector,
                            fn));
                    index++;
                }
            }
        }
        return byId;
    }

    /**
     * Performs the "original" checks Kube imposed on pre-{@code apiextensions.k8s.io/v1} CRD schemas
     * @see <a href="https://kubernetes.io/docs/tasks/extend-kubernetes/custom-resources/custom-resource-definitions/#validation">Kube docs JSON Schema restrictions</a>
     */
    private void checkKubeConstraints() {
        // Kube Schema restrictions

        // Disallowed keywords
        // definitions
        // dependencies
        // deprecated
        // discriminator
        // id
        // patternProperties
        // readOnly
        // writeOpne
        // xml
        // $ref

        // assert uniqueItems != true
        // assert additionalProperties != false
        // assert !(additionalProperties != null && properties != null)
        SchemaValidatorsConfig.Builder builder = SchemaValidatorsConfig.builder();
        JsonSchemaWalkListener disallowedKeywordsListener = null;
        builder.keywordWalkListener(DEFINITIONS, disallowedKeywordsListener);
        builder.keywordWalkListener(DEPENDENCIES, disallowedKeywordsListener);
        builder.keywordWalkListener(DEPRECATED, disallowedKeywordsListener);
        builder.keywordWalkListener(DISCRIMINATOR, disallowedKeywordsListener);
        builder.keywordWalkListener(ID, disallowedKeywordsListener);
        builder.keywordWalkListener(PATTERN_PROPERTIES, disallowedKeywordsListener);
        builder.keywordWalkListener(READ_ONLY, disallowedKeywordsListener);
        builder.keywordWalkListener(WRITE_ONLY, disallowedKeywordsListener);
        builder.keywordWalkListener(XML, disallowedKeywordsListener);
        builder.keywordWalkListener(REF, disallowedKeywordsListener);
        SchemaValidatorsConfig config = builder.build();

        //JsonSchemaFactory jsonSchemaFactory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V4);
        JsonSchemaFactory jsonSchemaFactory = JsonSchemaFactory
                .builder(JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V4))
                .schemaMappers(schemaMappers -> schemaMappers.mapPrefix("https://www.example.org/", "classpath:schema/"))
                .build();
        JsonSchema schema = jsonSchemaFactory.getSchema(root, config);
        ValidationResult walk = schema.walk(schema.createExecutionContext(), root, true);
    }

    /**
     * Performs the "structural schema" checks Kube imposes on {@code apiextensions.k8s.io/v1} CRD schemas
     * @see <a href="https://kubernetes.io/docs/tasks/extend-kubernetes/custom-resources/custom-resource-definitions/#specifying-a-structural-schema">Kube docs on structural schemas</a>
     */
    private void checkIsStructural() {

        // Structural schema limitations
        // 1. type=object => all properties and additionalProperties have a type, or x-kubernetes-int-or-string: true, or x-kubernetes-preserve-unknown-fields: true
        // 1. type=array => all items have a type, or x-kubernetes-int-or-string: true, or x-kubernetes-preserve-unknown-fields: true
        var x = ConfigValidator.visitSchemas("#", root,
                Collector.<String, List<String>, List<String>>of(
                    ArrayList::new,
                    List::add,
                    (l1, l2) -> {
                        l1.addAll(l2);
                        return l1;
                    }, null),
                    (path, schema) -> {

                        if (schema.isObject()) {
                            // all properties and additionalProperties have a type, or x-kubernetes-int-or-string: true, or x-kubernetes-preserve-unknown-fields: true
                        }
                        else if (schema.isArray()) {
                            // all items have a type, or x-kubernetes-int-or-string: true, or x-kubernetes-preserve-unknown-fields: true
                        }
                        return path;
                    });
        // 2. tricky

        // 3.
    }



}

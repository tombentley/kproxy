/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.bootstrap;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SchemaId;
import com.networknt.schema.SchemaLocation;
import com.networknt.schema.SchemaValidatorsConfig;
import com.networknt.schema.SpecVersion;
import com.networknt.schema.ValidationMessage;

/**
 * An immutable and valid schema for a configuration.
 * The validity rules are the same as for Kubernetes.
 * Instances can be created from classpath resources, or from string literals,
 * or by dynamically constructed via the {@link ConfigSchema#builder(String)}.
 */
public class ConfigSchema {

    static final YAMLMapper MAPPER = new YAMLMapper()
            .disable(YAMLGenerator.Feature.WRITE_DOC_START_MARKER);

    // Invariant: This is a valid schema
    // Enforced by: Constructor
    private final JsonNode rootNode;

    ConfigSchema(JsonNode rootNode) {
        Objects.requireNonNull(rootNode);
        validateDraft4(rootNode);
        this.rootNode = rootNode;
    }

    private static void validateDraft4(JsonNode rootNode) {
        JsonSchemaFactory jsonSchemaFactory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V4);

        SchemaValidatorsConfig.Builder builder = SchemaValidatorsConfig.builder();
        // By default the JDK regular expression implementation which is not ECMA 262 compliant is used
        // Note that setting this requires including optional dependencies
        // builder.regularExpressionFactory(GraalJSRegularExpressionFactory.getInstance());
        // builder.regularExpressionFactory(JoniRegularExpressionFactory.getInstance());
        SchemaValidatorsConfig config = builder.build();

        // Due to the mapping the meta-schema will be retrieved from the classpath at classpath:draft/2020-12/schema.
        JsonSchema schema = jsonSchemaFactory.getSchema(SchemaLocation.of(SchemaId.V202012), config);
        Set<ValidationMessage> assertions = schema.validate(rootNode, executionContext -> {
            // By default since Draft 2019-09 the format keyword only generates annotations and not assertions
            executionContext.getExecutionConfig().setFormatAssertionsEnabled(true);
        });
        if (!assertions.isEmpty()) {
            throw new RuntimeException("Not a valid schema: " + assertions);
        }
    }

    static ConfigSchema create(InputStream inputStream) throws IOException {
        var rootNode = MAPPER.readTree(inputStream);
        // TODO validate that the JSON is a Draft 4 schema
        // TODO validate the other kube restrictions

        return new ConfigSchema(rootNode);
    }

    static ConfigSchema create(String jsonSchema) throws IOException {
        YAMLMapper mapper = new YAMLMapper();
        var rootNode = mapper.readTree(jsonSchema);
        // TODO validate that the JSON is a Draft 4 schema
        // TODO validate the other kube restrictions

        return new ConfigSchema(rootNode);
    }

    public static ConfigSchema choice(Plugins plugins, Class<? extends Plugin> pluginPoint) {
        var choiceNode = MAPPER.getNodeFactory().objectNode();
        choiceNode.put("type", "object")
                .put("minProperties", 1)
                .put("maxProperties", 1);
        var propertiesNode = choiceNode.putObject("properties");
        for (Plugin plugin : plugins.implementationsOf(pluginPoint)) {
            ConfigSchema configSchema = plugin.configSchema(plugins);
            propertiesNode.set(plugins.choiceId(plugin), configSchema.rootNode);
        }

        // no need to validate, because this is a valid composition of valid schemas
        return new ConfigSchema(choiceNode);
    }

    public static Builder builder(ConfigSchema schema) {
        return new Builder(schema.rootNode.deepCopy());
    }

    public static Builder builder(String schemaAsString) {
        try {
            return new Builder(MAPPER.readTree(schemaAsString));
        }
        catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    public String toString() {
        try {
            return MAPPER.writeValueAsString(rootNode);
        }
        catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Validate the given configuration against this schema
     * @param configContent
     * @return A config (if this content was valid).
     * @throws InvalidConfigException If the given config was not valid to this schema.
     */
    public Config validateConfig(String configContent) {
        JsonNode configInstance = null;
        try {
            configInstance = MAPPER.readTree(configContent);
        }
        catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
        if (configInstance.has("$schema")
                && !configInstance.get("$schema").asText().equals("https://json-schema.org/draft-04/schema")) {
            throw new IllegalArgumentException("config refers to non-v4 JSON Schema");
        }
        JsonSchemaFactory jsonSchemaFactory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V4);

        SchemaValidatorsConfig.Builder builder = SchemaValidatorsConfig.builder();
        JsonSchema schema = jsonSchemaFactory.getSchema(this.rootNode);

        Set<ValidationMessage> assertions = schema.validate(configInstance, executionContext -> {
            // By default since Draft 2019-09 the format keyword only generates annotations and not assertions
            executionContext.getExecutionConfig().setFormatAssertionsEnabled(true);
        });

        if (!assertions.isEmpty()) {
            throw new RuntimeException("Invalid config: " + assertions);
        }

        return new Config(configInstance);
    }

    /**
     * A mutable, possibly not valid, schema that can be manipultated before
     * being turned into an immutable validated {@link ConfigSchema} using {@link #build()}.
     */
    public static class Builder {

        private final JsonNode rootNode;

        private Builder(JsonNode rootNode) {
            this.rootNode = rootNode;
        }

        public Builder replace(
                List<String> path,
                ConfigSchema kImpls) {
            if (path.isEmpty()) {
                throw new IllegalArgumentException("Path is empty");
            }
            var node = path(rootNode, path.subList(0, path.size() - 1));
            ((ObjectNode) node).replace(path.get(path.size() - 1), kImpls.rootNode);
            return this;
        }

        public ConfigSchema build() {
            return new ConfigSchema(rootNode);
        }

    }

    static JsonNode path(
            JsonNode node,
            List<String> path
    ) {
        for (String pathSegment: path) {
            if (node.isArray()) {
                node = node.path(Integer.parseInt(pathSegment));
            } else if (node.isObject()) {
                node = node.path(pathSegment);
            }
            if (node.isMissingNode()) {
                throw new RuntimeException("Node did not exist for path segment " + pathSegment);
            }
        }
        return node;
    }
}

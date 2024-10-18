/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.bootstrap;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.List;

public class ConfigSchemaTemplate {

    private final JsonNode rootNode;

    public ConfigSchemaTemplate(JsonNode rootNode) {
        this.rootNode = rootNode;
    }

    public static ConfigSchemaTemplate create(String schemaAsString) {
        try {
            return new ConfigSchemaTemplate(ConfigSchema.mapper.readTree(schemaAsString));
        }
        catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    public ConfigSchemaTemplate replace(
            List<String> path,
            ConfigSchema kImpls) {
        if (path.isEmpty()) {
            throw new IllegalArgumentException("Path is empty");
        }
        JsonNode jsonNode = rootNode.deepCopy();
        var node = jsonNode;
        for (String f : path.subList(0, path.size() - 1)) {
            if (node.isArray()) {
                node = node.path(Integer.parseInt(f));
            } else if (node.isObject()) {
                node = node.path(f);
            }
            if (node.isMissingNode()) {
                throw new RuntimeException("Node did not exist for path segment " + f);
            }
        }
        ((ObjectNode) node).replace(path.get(path.size() - 1), kImpls.rootNode);
        return new ConfigSchemaTemplate(jsonNode);
    }

    public ConfigSchema build() {
        return new ConfigSchema(rootNode);
    }

}

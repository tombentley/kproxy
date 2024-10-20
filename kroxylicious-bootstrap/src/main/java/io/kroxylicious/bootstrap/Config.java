/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.bootstrap;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.List;
import java.util.Objects;

/**
 * A validated configuration.
 * Validation is against some {@link ConfigSchema}.
 */
public class Config {

    private final JsonNode node;

    Config(JsonNode node) {
        Objects.requireNonNull(node);
        this.node = node;
    }

    public JsonNode toJsonNode() {
        return node.deepCopy();
    }

    public String toString() {
        return node.toString();
    }

    public <T> T toInstance(Class<T> cls) {
        return ConfigSchema.MAPPER.convertValue(node, cls);
    }

    public Config path(List<String> path) {
        return new Config(ConfigSchema.path(this.node, path));
    }
}

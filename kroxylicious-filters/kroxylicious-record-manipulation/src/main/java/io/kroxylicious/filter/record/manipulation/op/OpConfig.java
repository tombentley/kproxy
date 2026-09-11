/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.record.manipulation.op;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Names a pluggable operation and carries its configuration, not yet deserialized into a concrete type.
 * The input/output type {@code op} must resolve to (e.g. {@code String}-to-{@code String}) depends on
 * the primitive type of the field this operation applies to, which isn't known until the format-specific
 * engine walks its schema - so, unlike other plugin configuration in Kroxylicious, {@code config} is
 * deliberately left as an untyped property map rather than being resolved automatically via
 * {@code @PluginImplName}/{@code @PluginImplConfig}. See the resolving {@code OpFactory} implementations
 * for how {@code config} gets converted to a concrete type.
 * <p>
 *
 * @param op the name of the plugin implementation to use
 * @param config the operation's configuration properties, not yet deserialized into a concrete type
 */
public record OpConfig(@JsonProperty(required = true) String op, @JsonAnySetter Map<String, Object> config) {
    public OpConfig(Class<? extends OpFactory> op) {
        this(op, Map.of());
    }

    public OpConfig(Class<? extends OpFactory> op, Map<String, Object> config) {
        this(op.getName(), config);
    }
}

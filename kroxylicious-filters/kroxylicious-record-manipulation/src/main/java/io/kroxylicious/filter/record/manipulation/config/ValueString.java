/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.record.manipulation.config;

import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.kroxylicious.filter.record.manipulation.common.OpFactory;
import io.kroxylicious.filter.record.manipulation.common.TypedOp;
import io.kroxylicious.proxy.plugin.Plugin;

/**
 * Replaces a value with a fixed {@link String}.
 */
@Plugin(configType = ValueString.Config.class)
public class ValueString implements OpFactory<String, String> {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    /**
     * Configuration for {@link ValueString}.
     * @param value the constant value to use
     */
    public record Config(String value) {}

    @Override
    public TypedOp<String, String> create(Map<String, Object> configMap) {
        Config config = MAPPER.convertValue(configMap, Config.class);
        return TypedOp.of(String.class, (ignored, context) -> config.value());
    }
}

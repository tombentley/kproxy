/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.record.manipulation.config;

import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.kroxylicious.filter.record.manipulation.common.BooleanOp;
import io.kroxylicious.filter.record.manipulation.common.BooleanOpFactory;
import io.kroxylicious.proxy.plugin.Plugin;

/**
 * Replaces a value with a fixed {@link Integer}. See {@link ValueString} for the equivalent
 * {@link String} operation.
 */
@Plugin(configType = ValueBoolean.Config.class)
public class ValueBoolean implements BooleanOpFactory {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    /**
     * Configuration for {@link ValueBoolean}.
     * @param value the constant value to use
     */
    public record Config(boolean value) {}

    @Override
    public BooleanOp create(Map<String, Object> configMap) {
        Config config = MAPPER.convertValue(configMap, Config.class);
        return (ignored, context) -> config.value();
    }
}

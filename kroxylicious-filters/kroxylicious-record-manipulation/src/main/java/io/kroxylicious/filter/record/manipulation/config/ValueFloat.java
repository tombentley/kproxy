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
 * Replaces a value with a fixed {@link Float}.
 */
@Plugin(configType = ValueFloat.Config.class)
public class ValueFloat implements OpFactory<Float, Float> {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    /**
     * Configuration for {@link ValueFloat}.
     * @param value the constant value to use
     */
    public record Config(float value) {}

    @Override
    public TypedOp<Float, Float> create(Map<String, Object> configMap) {
        Config config = MAPPER.convertValue(configMap, Config.class);
        return TypedOp.of(Float.class, (ignored, context) -> config.value());
    }
}

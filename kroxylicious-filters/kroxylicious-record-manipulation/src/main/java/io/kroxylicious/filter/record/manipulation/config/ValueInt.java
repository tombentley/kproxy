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
 * Replaces a value with a fixed {@link Integer}.
 */
@Plugin(configType = ValueInt.Config.class)
public class ValueInt implements OpFactory<Integer, Integer> {

    /**
     * Configuration for {@link ValueInt}.
     * @param value the constant value to use
     */
    public record Config(int value) {}

    @Override
    public TypedOp<Integer, Integer> create(Map<String, Object> configMap) {
        Config config = OpConfigs.MAPPER.convertValue(configMap, Config.class);
        return TypedOp.of(Integer.class, (ignored, context) -> config.value());
    }
}

/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.record.manipulation.config;

import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.kroxylicious.filter.record.manipulation.common.OpFactory;
import io.kroxylicious.filter.record.manipulation.common.RandomFloatSupplier;
import io.kroxylicious.filter.record.manipulation.common.TypedOp;
import io.kroxylicious.proxy.plugin.Plugin;

/**
 * Generates a random {@link Float} drawn from a range.
 */
@Plugin(configType = RandomFloat.Config.class)
public class RandomFloat implements OpFactory<Float, Float> {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    /**
     * Configuration for {@link RandomFloat}.
     * @param minInclusive the minimum value (inclusive)
     * @param maxExclusive the maximum value (exclusive)
     */
    public record Config(float minInclusive, float maxExclusive) {}

    @Override
    public TypedOp<Float, Float> create(Map<String, Object> configMap) {
        Config config = MAPPER.convertValue(configMap, Config.class);
        var generator = new RandomFloatSupplier(config.minInclusive(), config.maxExclusive());
        return TypedOp.of(Float.class, (ignored, context) -> generator.apply(context));
    }
}

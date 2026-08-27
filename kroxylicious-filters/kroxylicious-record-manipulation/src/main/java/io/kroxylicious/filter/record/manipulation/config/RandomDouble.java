/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.record.manipulation.config;

import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.kroxylicious.filter.record.manipulation.common.OpFactory;
import io.kroxylicious.filter.record.manipulation.common.RandomDoubleSupplier;
import io.kroxylicious.filter.record.manipulation.common.TypedOp;
import io.kroxylicious.proxy.plugin.Plugin;

/**
 * Generates a random {@link Double} drawn from a range.
 */
@Plugin(configType = RandomDouble.Config.class)
public class RandomDouble implements OpFactory<Double, Double> {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    /**
     * Configuration for {@link RandomDouble}.
     * @param minInclusive the minimum value (inclusive)
     * @param maxExclusive the maximum value (exclusive)
     */
    public record Config(double minInclusive, double maxExclusive) {}

    @Override
    public TypedOp<Double, Double> create(Map<String, Object> configMap) {
        Config config = MAPPER.convertValue(configMap, Config.class);
        var generator = new RandomDoubleSupplier(config.minInclusive(), config.maxExclusive());
        return TypedOp.of(Double.class, (ignored, context) -> generator.applyAsDouble(context));
    }
}

/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.record.manipulation.ops.random;

import java.lang.reflect.Type;
import java.util.Map;

import io.kroxylicious.filter.record.manipulation.op.PluginLookup;
import io.kroxylicious.filter.record.manipulation.config.OpConfigs;
import io.kroxylicious.filter.record.manipulation.op.BaseTypedOp;
import io.kroxylicious.filter.record.manipulation.op.OpFactory;
import io.kroxylicious.proxy.plugin.Plugin;

/**
 * Generates a random {@link Float} drawn from a range.
 */
@Plugin(configType = RandomFloat.Config.class)
public class RandomFloat implements OpFactory<Float, Float> {

    /**
     * Configuration for {@link RandomFloat}.
     * @param minInclusive the minimum value (inclusive)
     * @param maxExclusive the maximum value (exclusive)
     */
    public record Config(float minInclusive, float maxExclusive) {}

    @Override
    public BaseTypedOp<Float, Float> create(Map<String, Object> configMap, PluginLookup lookup, Type argumentType) {
        Config config = Mapper.OP_CONFIG_MAPPER.convertValue(configMap, Config.class);
        var generator = new RandomFloatSupplier(config.minInclusive(), config.maxExclusive());
        return BaseTypedOp.of(Float.class, Float.class, (value, opContext) -> generator.apply(opContext));
    }
}

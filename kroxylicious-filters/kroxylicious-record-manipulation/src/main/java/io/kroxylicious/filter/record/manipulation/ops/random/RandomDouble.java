/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.record.manipulation.ops.random;

import java.lang.reflect.Type;
import java.util.Map;

import io.kroxylicious.filter.record.manipulation.config.OpConfigs;
import io.kroxylicious.filter.record.manipulation.op.BaseTypedOp;
import io.kroxylicious.filter.record.manipulation.op.OpContext;
import io.kroxylicious.filter.record.manipulation.op.OpFactory;
import io.kroxylicious.filter.record.manipulation.common.PluginLookup;
import io.kroxylicious.filter.record.manipulation.common.StaticTypedOp;
import io.kroxylicious.proxy.plugin.Plugin;

/**
 * Generates a random {@link Double} drawn from a range.
 */
@Plugin(configType = RandomDouble.Config.class)
public class RandomDouble implements OpFactory<Double, Double> {

    /**
     * Configuration for {@link RandomDouble}.
     * @param minInclusive the minimum value (inclusive)
     * @param maxExclusive the maximum value (exclusive)
     */
    public record Config(double minInclusive, double maxExclusive) {}

    @Override
    public BaseTypedOp<Double, Double> create(Map<String, Object> configMap, PluginLookup lookup, Type argumentType) {
        Config config = OpConfigs.OP_CONFIG_MAPPER.convertValue(configMap, Config.class);
        var generator = new RandomDoubleSupplier(config.minInclusive(), config.maxExclusive());
        return new StaticTypedOp<Double, Double>() {
            @Override
            public Type outputType(Type inputType) {
                return Double.class;
            }

            @Override
            public Double apply(Double value, OpContext opContext) {
                return generator.applyAsDouble(opContext);
            }
        };
    }
}

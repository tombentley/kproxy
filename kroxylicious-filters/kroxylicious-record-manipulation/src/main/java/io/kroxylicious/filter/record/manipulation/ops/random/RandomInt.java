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
 * Generates a random {@link Integer} drawn from a range.
 */
@Plugin(configType = RandomInt.Config.class)
public class RandomInt implements OpFactory<Integer, Integer> {

    /**
     * Configuration for {@link RandomInt}.
     * @param minInclusive the minimum value (inclusive)
     * @param maxExclusive the maximum value (exclusive)
     */
    public record Config(int minInclusive, int maxExclusive) {}

    @Override
    public BaseTypedOp<Integer, Integer> create(Map<String, Object> configMap, PluginLookup lookup, Type argumentType) {
        Config config = OpConfigs.OP_CONFIG_MAPPER.convertValue(configMap, Config.class);
        var generator = new RandomIntSupplier(config.minInclusive(), config.maxExclusive());
        return new StaticTypedOp<Integer, Integer>() {
            @Override
            public Type outputType(Type inputType) {
                return Integer.class;
            }

            @Override
            public Integer apply(Integer value, OpContext opContext) {
                return generator.applyAsInt(opContext);
            }
        };
    }
}

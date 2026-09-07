/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.record.manipulation.config;

import java.lang.reflect.Type;
import java.util.Map;

import io.kroxylicious.filter.record.manipulation.op.BaseTypedOp;
import io.kroxylicious.filter.record.manipulation.op.OpContext;
import io.kroxylicious.filter.record.manipulation.op.OpFactory;
import io.kroxylicious.filter.record.manipulation.common.PluginLookup;
import io.kroxylicious.filter.record.manipulation.common.RandomLongSupplier;
import io.kroxylicious.filter.record.manipulation.common.StaticTypedOp;
import io.kroxylicious.proxy.plugin.Plugin;

/**
 * Generates a random {@link Long} drawn from a range.
 */
@Plugin(configType = RandomLong.Config.class)
public class RandomLong implements OpFactory<Long, Long> {

    /**
     * Configuration for {@link RandomLong}.
     * @param minInclusive the minimum value (inclusive)
     * @param maxExclusive the maximum value (exclusive)
     */
    public record Config(long minInclusive, long maxExclusive) {}

    @Override
    public BaseTypedOp<Long, Long> create(Map<String, Object> configMap, PluginLookup lookup, Type argumentType) {
        Config config = OpConfigs.OP_CONFIG_MAPPER.convertValue(configMap, Config.class);
        var generator = new RandomLongSupplier(config.minInclusive(), config.maxExclusive());
        return new StaticTypedOp<Long, Long>() {
            @Override
            public Type outputType(Type inputType) {
                return Long.class;
            }

            @Override
            public Long apply(Long value, OpContext opContext) {
                return generator.applyAsLong(opContext);
            }
        };
    }
}

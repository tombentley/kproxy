/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.record.manipulation.config;

import java.lang.reflect.Type;
import java.util.Map;

import io.kroxylicious.filter.record.manipulation.common.BaseTypedOp;
import io.kroxylicious.filter.record.manipulation.common.OpContext;
import io.kroxylicious.filter.record.manipulation.common.OpFactory;
import io.kroxylicious.filter.record.manipulation.common.PluginLookup;
import io.kroxylicious.filter.record.manipulation.common.StaticTypedOp;
import io.kroxylicious.proxy.plugin.Plugin;

/**
 * Replaces a value with a fixed {@link Long}.
 */
@Plugin(configType = ValueLong.Config.class)
public class ValueLong implements OpFactory<Long, Long> {

    /**
     * Configuration for {@link ValueLong}.
     * @param value the constant value to use
     */
    public record Config(long value) {}

    @Override
    public BaseTypedOp<Long, Long> create(Map<String, Object> configMap, PluginLookup lookup, Type argumentType) {
        Config config = OpConfigs.OP_CONFIG_MAPPER.convertValue(configMap, Config.class);
        return new StaticTypedOp<Long, Long>() {
            @Override
            public Type outputType(Type inputType) {
                return Long.class;
            }

            @Override
            public Long apply(Long value, OpContext opContext) {
                return config.value();
            }
        };
    }
}

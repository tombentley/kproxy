/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.record.manipulation.ops.constant;

import java.lang.reflect.Type;
import java.util.Map;

import io.kroxylicious.filter.record.manipulation.common.PluginLookup;
import io.kroxylicious.filter.record.manipulation.common.StaticTypedOp;
import io.kroxylicious.filter.record.manipulation.config.OpConfigs;
import io.kroxylicious.filter.record.manipulation.op.BaseTypedOp;
import io.kroxylicious.filter.record.manipulation.op.OpContext;
import io.kroxylicious.filter.record.manipulation.op.OpFactory;
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
    public BaseTypedOp<Integer, Integer> create(Map<String, Object> configMap, PluginLookup lookup, Type argumentType) {
        Config config = OpConfigs.OP_CONFIG_MAPPER.convertValue(configMap, Config.class);
        return new StaticTypedOp<Integer, Integer>() {
            @Override
            public Type outputType(Type inputType) {
                return Integer.class;
            }

            @Override
            public Integer apply(Integer value, OpContext opContext) {
                return config.value();
            }
        };
    }
}

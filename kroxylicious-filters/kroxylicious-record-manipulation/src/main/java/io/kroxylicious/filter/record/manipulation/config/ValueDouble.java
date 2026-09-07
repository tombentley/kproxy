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
 * Replaces a value with a fixed {@link Double}.
 */
@Plugin(configType = ValueDouble.Config.class)
public class ValueDouble implements OpFactory<Double, Double> {

    /**
     * Configuration for {@link ValueDouble}.
     * @param value the constant value to use
     */
    public record Config(double value) {}

    @Override
    public BaseTypedOp<Double, Double> create(Map<String, Object> configMap, PluginLookup lookup, Type argumentType) {
        Config config = OpConfigs.OP_CONFIG_MAPPER.convertValue(configMap, Config.class);
        return new StaticTypedOp<Double, Double>() {
            @Override
            public Type outputType(Type inputType) {
                return Double.class;
            }

            @Override
            public Double apply(Double value, OpContext opContext) {
                return config.value();
            }
        };
    }
}

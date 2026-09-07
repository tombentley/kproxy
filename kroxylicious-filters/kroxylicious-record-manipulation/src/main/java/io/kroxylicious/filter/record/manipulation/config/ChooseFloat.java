/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.record.manipulation.config;

import java.lang.reflect.Type;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import io.kroxylicious.filter.record.manipulation.common.BaseTypedOp;
import io.kroxylicious.filter.record.manipulation.common.ChooseFloatSupplier;
import io.kroxylicious.filter.record.manipulation.common.OpContext;
import io.kroxylicious.filter.record.manipulation.common.OpFactory;
import io.kroxylicious.filter.record.manipulation.common.PluginLookup;
import io.kroxylicious.filter.record.manipulation.common.StaticTypedOp;
import io.kroxylicious.proxy.plugin.Plugin;

/**
 * Generates a random {@link Float} drawn from a fixed set.
 */
@Plugin(configType = ChooseFloat.Config.class)
public class ChooseFloat implements OpFactory<Float, Float> {

    /**
     * Configuration for {@link ChooseFloat}.
     * @param from the set of values to choose from
     */
    public record Config(List<Float> from) {}

    @Override
    public BaseTypedOp<Float, Float> create(Map<String, Object> configMap, PluginLookup lookup, Type argumentType) {
        Config config = OpConfigs.OP_CONFIG_MAPPER.convertValue(configMap, Config.class);
        var generator = new ChooseFloatSupplier(new HashSet<>(config.from()));
        return new StaticTypedOp<Float, Float>() {
            @Override
            public Type outputType(Type inputType) {
                return Float.class;
            }

            @Override
            public Float apply(Float value, OpContext opContext) {
                return generator.apply(opContext);
            }
        };
    }
}

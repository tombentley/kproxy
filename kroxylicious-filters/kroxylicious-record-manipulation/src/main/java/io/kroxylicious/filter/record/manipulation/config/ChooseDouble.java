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
import io.kroxylicious.filter.record.manipulation.common.ChooseDoubleSupplier;
import io.kroxylicious.filter.record.manipulation.common.OpFactory;
import io.kroxylicious.filter.record.manipulation.common.PluginLookup;
import io.kroxylicious.proxy.plugin.Plugin;

/**
 * Generates a random {@link Integer} drawn from a fixed set.
 */
@Plugin(configType = ChooseDouble.Config.class)
public class ChooseDouble implements OpFactory<Object, Double> {

    /**
     * Configuration for {@link ChooseDouble}.
     * @param from the set of values to choose from
     */
    public record Config(List<Double> from) {}

    @Override
    public BaseTypedOp<Object, Double> create(Map<String, Object> configMap, PluginLookup lookup, Type argumentType) {
        Config config = OpConfigs.OP_CONFIG_MAPPER.convertValue(configMap, Config.class);
        var generator = new ChooseDoubleSupplier(new HashSet<>(config.from()));
        return BaseTypedOp.of(Object.class, Double.class, generator);
    }
}

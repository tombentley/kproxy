/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.record.manipulation.ops.choose;

import java.lang.reflect.Type;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import io.kroxylicious.filter.record.manipulation.op.PluginLookup;
import io.kroxylicious.filter.record.manipulation.config.OpConfigs;
import io.kroxylicious.filter.record.manipulation.op.BaseTypedOp;
import io.kroxylicious.filter.record.manipulation.op.OpFactory;
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
        Config config = Mapper.OP_CONFIG_MAPPER.convertValue(configMap, Config.class);
        var generator = new ChooseFloatSupplier(new HashSet<>(config.from()));
        return BaseTypedOp.of(Float.class, Float.class, (value, opContext) -> generator.apply(opContext));
    }
}

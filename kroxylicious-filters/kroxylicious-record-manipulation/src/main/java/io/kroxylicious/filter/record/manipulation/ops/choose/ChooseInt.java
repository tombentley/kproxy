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
 * Generates a random {@link Integer} drawn from a fixed set.
 */
@Plugin(configType = ChooseInt.Config.class)
public class ChooseInt implements OpFactory<Integer, Integer> {

    /**
     * Configuration for {@link ChooseInt}.
     * @param from the set of values to choose from
     */
    public record Config(List<Integer> from) {}

    @Override
    public BaseTypedOp<Integer, Integer> create(Map<String, Object> configMap, PluginLookup lookup, Type argumentType) {
        Config config = Mapper.OP_CONFIG_MAPPER.convertValue(configMap, Config.class);
        var generator = new ChooseIntSupplier(new HashSet<>(config.from()));
        return BaseTypedOp.of(Integer.class, Integer.class, (value, opContext) -> generator.applyAsInt(opContext));
    }
}

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

import io.kroxylicious.filter.record.manipulation.config.OpConfigs;
import io.kroxylicious.filter.record.manipulation.op.BaseTypedOp;
import io.kroxylicious.filter.record.manipulation.op.OpContext;
import io.kroxylicious.filter.record.manipulation.op.OpFactory;
import io.kroxylicious.filter.record.manipulation.common.PluginLookup;
import io.kroxylicious.filter.record.manipulation.common.StaticTypedOp;
import io.kroxylicious.proxy.plugin.Plugin;

/**
 * Generates a random {@link String} drawn from a fixed set.
 */
@Plugin(configType = ChooseString.Config.class)
public class ChooseString implements OpFactory<String, String> {

    /**
     * Configuration for {@link ChooseString}.
     * @param from the set of values to choose from
     */
    public record Config(List<String> from) {}

    @Override
    public BaseTypedOp<String, String> create(Map<String, Object> configMap, PluginLookup lookup, Type argumentType) {
        Config config = OpConfigs.OP_CONFIG_MAPPER.convertValue(configMap, Config.class);
        var generator = new ChooseStringSupplier(new HashSet<>(config.from()));
        return new StaticTypedOp<String, String>() {
            @Override
            public Type outputType(Type inputType) {
                return String.class;
            }

            @Override
            public String apply(String value, OpContext opContext) {
                return generator.apply(opContext);
            }
        };
    }
}

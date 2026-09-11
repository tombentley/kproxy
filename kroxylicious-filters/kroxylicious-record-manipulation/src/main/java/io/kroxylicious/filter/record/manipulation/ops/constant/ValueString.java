/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.record.manipulation.ops.constant;

import java.lang.reflect.Type;
import java.util.Map;

import io.kroxylicious.filter.record.manipulation.op.PluginLookup;
import io.kroxylicious.filter.record.manipulation.config.OpConfigs;
import io.kroxylicious.filter.record.manipulation.op.BaseTypedOp;
import io.kroxylicious.filter.record.manipulation.op.OpFactory;
import io.kroxylicious.proxy.plugin.Plugin;

/**
 * Replaces a value with a fixed {@link String}.
 */
@Plugin(configType = ValueString.Config.class)
public class ValueString implements OpFactory<Object, String> {

    /**
     * Configuration for {@link ValueString}.
     * @param value the constant value to use
     */
    public record Config(String value) {}

    @Override
    public BaseTypedOp<Object, String> create(Map<String, Object> configMap, PluginLookup lookup, Type argumentType) {
        Config config = Mapper.OP_CONFIG_MAPPER.convertValue(configMap, Config.class);
        return BaseTypedOp.of(Object.class, String.class, (value, opContext) -> config.value());
    }
}

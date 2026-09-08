/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.record.manipulation.ops.constant;

import java.lang.reflect.Type;
import java.util.Map;

import io.kroxylicious.filter.record.manipulation.common.PluginLookup;
import io.kroxylicious.filter.record.manipulation.config.OpConfigs;
import io.kroxylicious.filter.record.manipulation.op.BaseTypedOp;
import io.kroxylicious.filter.record.manipulation.op.OpFactory;
import io.kroxylicious.proxy.plugin.Plugin;

/**
 * Replaces a value with a fixed {@link Boolean}.
 */
@Plugin(configType = ValueBoolean.Config.class)
public class ValueBoolean implements OpFactory<Boolean, Boolean> {

    /**
     * Configuration for {@link ValueBoolean}.
     * @param value the constant value to use
     */
    public record Config(boolean value) {}

    @Override
    public BaseTypedOp<Boolean, Boolean> create(Map<String, Object> configMap, PluginLookup lookup, Type argumentType) {
        Config config = OpConfigs.OP_CONFIG_MAPPER.convertValue(configMap, Config.class);
        return BaseTypedOp.of(Boolean.class, Boolean.class, (value, opContext) -> config.value());
    }
}

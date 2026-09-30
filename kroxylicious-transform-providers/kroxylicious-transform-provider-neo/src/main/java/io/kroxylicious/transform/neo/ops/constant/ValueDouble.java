/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.transform.neo.ops.constant;

import java.lang.reflect.Type;
import java.util.Map;

import io.kroxylicious.proxy.plugin.Plugin;
import io.kroxylicious.transform.neo.op.BaseTypedOp;
import io.kroxylicious.transform.neo.op.OpFactory;
import io.kroxylicious.transform.service.PluginLookup;

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
        Config config = Mapper.OP_CONFIG_MAPPER.convertValue(configMap, Config.class);
        return BaseTypedOp.of(Double.class, Double.class, (value, opContext) -> config.value());
    }
}

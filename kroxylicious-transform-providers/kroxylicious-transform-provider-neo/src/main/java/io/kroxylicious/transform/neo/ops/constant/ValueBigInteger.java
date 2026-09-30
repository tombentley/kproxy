/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.transform.neo.ops.constant;

import java.lang.reflect.Type;
import java.math.BigInteger;
import java.util.Map;

import io.kroxylicious.proxy.plugin.Plugin;
import io.kroxylicious.transform.neo.op.BaseTypedOp;
import io.kroxylicious.transform.neo.op.OpFactory;
import io.kroxylicious.transform.service.PluginLookup;

/**
 * Replaces a value with a fixed {@link Boolean}.
 */
@Plugin(configType = ValueBigInteger.Config.class)
public class ValueBigInteger implements OpFactory<BigInteger, BigInteger> {

    /**
     * Configuration for {@link ValueBoolean}.
     * @param value the constant value to use
     */
    public record Config(BigInteger value) {}

    @Override
    public BaseTypedOp<BigInteger, BigInteger> create(Map<String, Object> configMap, PluginLookup lookup, Type argumentType) {
        Config config = Mapper.OP_CONFIG_MAPPER.convertValue(configMap, Config.class);
        return BaseTypedOp.of(BigInteger.class, BigInteger.class, (value, opContext) -> config.value());
    }
}

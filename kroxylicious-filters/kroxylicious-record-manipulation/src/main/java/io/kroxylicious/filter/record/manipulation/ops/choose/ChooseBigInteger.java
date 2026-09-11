/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.record.manipulation.ops.choose;

import java.lang.reflect.Type;
import java.math.BigInteger;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import io.kroxylicious.filter.record.manipulation.op.PluginLookup;
import io.kroxylicious.filter.record.manipulation.config.OpConfigs;
import io.kroxylicious.filter.record.manipulation.op.BaseTypedOp;
import io.kroxylicious.filter.record.manipulation.op.OpFactory;
import io.kroxylicious.proxy.plugin.Plugin;

/**
 * Generates a random {@link BigInteger} drawn from a fixed set.
 */
@Plugin(configType = ChooseBigInteger.Config.class)
public class ChooseBigInteger implements OpFactory<BigInteger, BigInteger> {



    /**
     * Configuration for {@link ChooseBigInteger}.
     * @param from the set of values to choose from
     */
    public record Config(List<BigInteger> from) {}

    @Override
    public BaseTypedOp<BigInteger, BigInteger> create(Map<String, Object> configMap, PluginLookup lookup, Type argumentType) {
        Config config = Mapper.OP_CONFIG_MAPPER.convertValue(configMap, Config.class);
        var generator = new ChooseBigIntegerSupplier(new HashSet<>(config.from()));
        return BaseTypedOp.of(BigInteger.class, BigInteger.class, generator);
    }
}

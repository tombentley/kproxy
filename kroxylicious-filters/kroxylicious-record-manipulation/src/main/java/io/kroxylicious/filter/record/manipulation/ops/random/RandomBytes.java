/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.record.manipulation.ops.random;

import java.lang.reflect.Type;
import java.util.Map;

import io.kroxylicious.filter.record.manipulation.config.OpConfigs;
import io.kroxylicious.filter.record.manipulation.op.BaseTypedOp;
import io.kroxylicious.filter.record.manipulation.op.OpContext;
import io.kroxylicious.filter.record.manipulation.op.OpFactory;
import io.kroxylicious.filter.record.manipulation.common.PluginLookup;
import io.kroxylicious.filter.record.manipulation.common.StaticTypedOp;
import io.kroxylicious.proxy.plugin.Plugin;

/**
 * Generates a random {@code byte[]} of a random length.
 */
@Plugin(configType = RandomBytes.Config.class)
public class RandomBytes implements OpFactory<byte[], byte[]> {

    /**
     * Configuration for {@link RandomBytes}.
     * @param minLengthInclusive the minimum length of the array (inclusive)
     * @param maxLengthExclusive the maximum length of the array (exclusive)
     */
    public record Config(int minLengthInclusive, int maxLengthExclusive) {}

    @Override
    public BaseTypedOp<byte[], byte[]> create(Map<String, Object> configMap, PluginLookup lookup, Type argumentType) {
        Config config = OpConfigs.OP_CONFIG_MAPPER.convertValue(configMap, Config.class);
        var generator = new RandomBytesSupplier(config.minLengthInclusive(), config.maxLengthExclusive());
        return new StaticTypedOp<byte[], byte[]>() {
            @Override
            public Type outputType(Type inputType) {
                return byte[].class;
            }

            @Override
            public byte[] apply(byte[] value, OpContext opContext) {
                return generator.apply(opContext);
            }
        };
    }
}

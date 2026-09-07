/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.record.manipulation.ops.constant;

import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.Map;

import io.kroxylicious.filter.record.manipulation.common.PluginLookup;
import io.kroxylicious.filter.record.manipulation.common.StaticTypedOp;
import io.kroxylicious.filter.record.manipulation.config.OpConfigs;
import io.kroxylicious.filter.record.manipulation.op.BaseTypedOp;
import io.kroxylicious.filter.record.manipulation.op.OpContext;
import io.kroxylicious.filter.record.manipulation.op.OpFactory;
import io.kroxylicious.proxy.plugin.Plugin;

/**
 * Replaces a value with a fixed {@code byte[]}.
 */
@Plugin(configType = ValueBytes.Config.class)
public class ValueBytes implements OpFactory<byte[], byte[]> {

    /**
     * Configuration for {@link ValueBytes}.
     * @param value the constant value to use
     */
    @SuppressWarnings("ArrayRecordComponent")
    public record Config(byte[] value) {
        @Override
        public boolean equals(Object other) {
            if (other instanceof Config(byte[] otherValue)) {
                return Arrays.equals(value, otherValue);
            }
            return false;

        }

        @Override
        public int hashCode() {
            return Arrays.hashCode(value);
        }

        @Override
        public String toString() {
            return "Config{" +
                    "value=" + Arrays.toString(value) +
                    '}';
        }
    }

    @Override
    public BaseTypedOp<byte[], byte[]> create(Map<String, Object> configMap, PluginLookup lookup, Type argumentType) {
        Config config = OpConfigs.OP_CONFIG_MAPPER.convertValue(configMap, Config.class);
        return new StaticTypedOp<byte[], byte[]>() {
            @Override
            public Type outputType(Type inputType) {
                return byte[].class;
            }

            @Override
            public byte[] apply(byte[] value, OpContext opContext) {
                return config.value();
            }
        };
    }
}

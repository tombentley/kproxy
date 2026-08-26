/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.record.manipulation.config;

import java.util.Arrays;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.kroxylicious.filter.record.manipulation.common.BytesOp;
import io.kroxylicious.filter.record.manipulation.common.BytesOpFactory;
import io.kroxylicious.proxy.plugin.Plugin;

/**
 * Replaces a value with a fixed {@code byte[]}.
 */
@Plugin(configType = ValueBytes.Config.class)
public class ValueBytes implements BytesOpFactory {

    private static final ObjectMapper MAPPER = new ObjectMapper();

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
    public BytesOp create(Map<String, Object> configMap) {
        Config config = MAPPER.convertValue(configMap, Config.class);
        return (ignored, context) -> config.value();
    }
}

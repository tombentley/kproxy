/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.record.manipulation.config;

import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.kroxylicious.filter.record.manipulation.common.OpFactory;
import io.kroxylicious.filter.record.manipulation.common.RandomBytesSupplier;
import io.kroxylicious.filter.record.manipulation.common.TypedOp;
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
    public TypedOp<byte[], byte[]> create(Map<String, Object> configMap) {
        Config config = OpConfigs.MAPPER.convertValue(configMap, Config.class);
        var generator = new RandomBytesSupplier(config.minLengthInclusive(), config.maxLengthExclusive());
        return TypedOp.of(byte[].class, (ignored, context) -> generator.apply(context));
    }
}

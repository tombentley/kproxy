/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.record.manipulation.config;

import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.kroxylicious.filter.record.manipulation.common.LongOp;
import io.kroxylicious.filter.record.manipulation.common.LongOpFactory;
import io.kroxylicious.filter.record.manipulation.common.RandomLongSupplier;
import io.kroxylicious.proxy.plugin.Plugin;

/**
 * Generates a random {@link Long} drawn from a range.
 */
@Plugin(configType = RandomLong.Config.class)
public class RandomLong implements LongOpFactory {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    /**
     * Configuration for {@link RandomLong}.
     * @param minInclusive the minimum value (inclusive)
     * @param maxExclusive the maximum value (exclusive)
     */
    public record Config(long minInclusive, long maxExclusive) {}

    @Override
    public LongOp create(Map<String, Object> configMap) {
        Config config = MAPPER.convertValue(configMap, Config.class);
        var generator = new RandomLongSupplier(config.minInclusive(), config.maxExclusive());
        return (ignored, context) -> generator.applyAsLong(context);
    }
}

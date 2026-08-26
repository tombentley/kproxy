/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.record.manipulation.config;

import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.kroxylicious.filter.record.manipulation.common.IntOp;
import io.kroxylicious.filter.record.manipulation.common.IntOpFactory;
import io.kroxylicious.filter.record.manipulation.common.RandomIntSupplier;
import io.kroxylicious.proxy.plugin.Plugin;

/**
 * Generates a random {@link Integer} drawn from a range.
 */
@Plugin(configType = RandomInt.Config.class)
public class RandomInt implements IntOpFactory {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    /**
     * Configuration for {@link RandomInt}.
     * @param minInclusive the minimum value (inclusive)
     * @param maxExclusive the maximum value (exclusive)
     */
    public record Config(int minInclusive, int maxExclusive) {}

    @Override
    public IntOp create(Map<String, Object> configMap) {
        Config config = MAPPER.convertValue(configMap, Config.class);
        var generator = new RandomIntSupplier(config.minInclusive(), config.maxExclusive());
        return (ignored, context) -> generator.applyAsInt(context);
    }
}

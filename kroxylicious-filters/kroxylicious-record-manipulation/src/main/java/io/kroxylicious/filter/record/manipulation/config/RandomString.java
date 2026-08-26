/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.record.manipulation.config;

import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.kroxylicious.filter.record.manipulation.common.RandomStringSupplier;
import io.kroxylicious.filter.record.manipulation.common.StringOp;
import io.kroxylicious.filter.record.manipulation.common.StringOpFactory;
import io.kroxylicious.proxy.plugin.Plugin;

/**
 * Generates a random {@link String} of a random length composed of codepoints taken from an alphabet.
 */
@Plugin(configType = RandomString.Config.class)
public class RandomString implements StringOpFactory {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    /**
     * Configuration for {@link RandomString}.
     * @param alphabet the codepoints to pick from
     * @param minLengthInclusive the minimum length of the string (inclusive)
     * @param maxLengthExclusive the maximum length of the string (exclusive)
     */
    public record Config(String alphabet, int minLengthInclusive, int maxLengthExclusive) {}

    @Override
    public StringOp create(Map<String, Object> configMap) {
        Config config = MAPPER.convertValue(configMap, Config.class);
        var generator = new RandomStringSupplier(config.alphabet(), config.minLengthInclusive(), config.maxLengthExclusive());
        return (ignored, context) -> generator.apply(context);
    }
}

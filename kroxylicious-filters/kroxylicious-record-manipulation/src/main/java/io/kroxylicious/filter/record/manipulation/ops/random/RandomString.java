/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.record.manipulation.ops.random;

import java.lang.reflect.Type;
import java.util.Map;

import io.kroxylicious.filter.record.manipulation.op.PluginLookup;
import io.kroxylicious.filter.record.manipulation.config.OpConfigs;
import io.kroxylicious.filter.record.manipulation.op.BaseTypedOp;
import io.kroxylicious.filter.record.manipulation.op.OpFactory;
import io.kroxylicious.proxy.plugin.Plugin;

/**
 * Generates a random {@link String} of a random length composed of codepoints taken from an alphabet.
 */
@Plugin(configType = RandomString.Config.class)
public class RandomString implements OpFactory<String, String> {

    /**
     * Configuration for {@link RandomString}.
     * @param alphabet the codepoints to pick from
     * @param minLengthInclusive the minimum length of the string (inclusive)
     * @param maxLengthExclusive the maximum length of the string (exclusive)
     */
    public record Config(String alphabet, int minLengthInclusive, int maxLengthExclusive) {}

    @Override
    public BaseTypedOp<String, String> create(Map<String, Object> configMap, PluginLookup lookup, Type argumentType) {
        Config config = Mapper.OP_CONFIG_MAPPER.convertValue(configMap, Config.class);
        var generator = new RandomStringSupplier(config.alphabet(), config.minLengthInclusive(), config.maxLengthExclusive());
        return BaseTypedOp.of(String.class, String.class, (value, opContext) -> generator.apply(opContext));
    }
}

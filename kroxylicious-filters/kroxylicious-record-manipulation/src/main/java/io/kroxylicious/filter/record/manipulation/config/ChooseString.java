/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.record.manipulation.config;

import java.util.HashSet;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.kroxylicious.filter.record.manipulation.common.ChooseStringSupplier;
import io.kroxylicious.filter.record.manipulation.common.StringOp;
import io.kroxylicious.filter.record.manipulation.common.StringOpFactory;
import io.kroxylicious.proxy.plugin.Plugin;

/**
 * Generates a random {@link String} drawn from a fixed set. See {@link ChooseInt} for the equivalent
 * {@link Integer} operation.
 */
@Plugin(configType = ChooseString.Config.class)
public class ChooseString implements StringOpFactory {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    /**
     * Configuration for {@link ChooseString}.
     * @param from the set of values to choose from
     */
    public record Config(List<String> from) {}

    @Override
    public StringOp create(Map<String, Object> configMap) {
        Config config = MAPPER.convertValue(configMap, Config.class);
        var generator = new ChooseStringSupplier(new HashSet<>(config.from()));
        return (ignored, context) -> generator.apply(context);
    }
}

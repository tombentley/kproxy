/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.record.manipulation.config;

import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.kroxylicious.filter.record.manipulation.common.BooleanOp;
import io.kroxylicious.filter.record.manipulation.common.BooleanOpFactory;
import io.kroxylicious.filter.record.manipulation.common.RandomBooleanSupplier;
import io.kroxylicious.proxy.plugin.Plugin;

/**
 * Generates a random {@link Integer} drawn from a range.
 */
@Plugin(configType = RandomBoolean.Config.class)
public class RandomBoolean implements BooleanOpFactory {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    /**
     * Configuration for {@link RandomBoolean}.
     */
    public record Config() {}

    @Override
    public BooleanOp create(Map<String, Object> configMap) {
        var generator = new RandomBooleanSupplier();
        return (ignored, context) -> generator.test(context);
    }
}

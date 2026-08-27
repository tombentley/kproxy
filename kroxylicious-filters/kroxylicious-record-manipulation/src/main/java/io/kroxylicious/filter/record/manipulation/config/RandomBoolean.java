/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.record.manipulation.config;

import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.kroxylicious.filter.record.manipulation.common.OpFactory;
import io.kroxylicious.filter.record.manipulation.common.RandomBooleanSupplier;
import io.kroxylicious.filter.record.manipulation.common.TypedOp;
import io.kroxylicious.proxy.plugin.Plugin;

/**
 * Generates a random {@link Integer} drawn from a range.
 */
@Plugin(configType = RandomBoolean.Config.class)
public class RandomBoolean implements OpFactory<Boolean, Boolean> {

    /**
     * Configuration for {@link RandomBoolean}.
     */
    public record Config() {}

    @Override
    public TypedOp<Boolean, Boolean> create(Map<String, Object> configMap) {
        var generator = new RandomBooleanSupplier();
        return TypedOp.of(Boolean.class, (ignored, context) -> generator.test(context));
    }
}

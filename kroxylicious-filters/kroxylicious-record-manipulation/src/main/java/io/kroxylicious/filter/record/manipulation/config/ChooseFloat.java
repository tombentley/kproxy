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

import io.kroxylicious.filter.record.manipulation.common.ChooseFloatSupplier;
import io.kroxylicious.filter.record.manipulation.common.FloatOp;
import io.kroxylicious.filter.record.manipulation.common.FloatOpFactory;
import io.kroxylicious.proxy.plugin.Plugin;

/**
 * Generates a random {@link Float} drawn from a fixed set.
 */
@Plugin(configType = ChooseFloat.Config.class)
public class ChooseFloat implements FloatOpFactory {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    /**
     * Configuration for {@link ChooseFloat}.
     * @param from the set of values to choose from
     */
    public record Config(List<Float> from) {}

    @Override
    public FloatOp create(Map<String, Object> configMap) {
        Config config = MAPPER.convertValue(configMap, Config.class);
        var generator = new ChooseFloatSupplier(new HashSet<>(config.from()));
        return (ignored, context) -> generator.apply(context);
    }
}

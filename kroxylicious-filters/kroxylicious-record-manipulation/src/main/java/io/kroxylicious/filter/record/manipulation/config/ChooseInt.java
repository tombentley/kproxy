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

import io.kroxylicious.filter.record.manipulation.common.ChooseIntSupplier;
import io.kroxylicious.filter.record.manipulation.common.OpFactory;
import io.kroxylicious.filter.record.manipulation.common.TypedOp;
import io.kroxylicious.proxy.plugin.Plugin;

/**
 * Generates a random {@link Integer} drawn from a fixed set.
 */
@Plugin(configType = ChooseInt.Config.class)
public class ChooseInt implements OpFactory<Integer, Integer> {

    /**
     * Configuration for {@link ChooseInt}.
     * @param from the set of values to choose from
     */
    public record Config(List<Integer> from) {}

    @Override
    public TypedOp<Integer, Integer> create(Map<String, Object> configMap) {
        Config config = OpConfigs.MAPPER.convertValue(configMap, Config.class);
        var generator = new ChooseIntSupplier(new HashSet<>(config.from()));
        return TypedOp.of(Integer.class, (ignored, context) -> generator.applyAsInt(context));
    }
}

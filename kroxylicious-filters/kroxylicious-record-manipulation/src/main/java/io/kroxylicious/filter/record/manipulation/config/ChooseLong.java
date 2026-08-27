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

import io.kroxylicious.filter.record.manipulation.common.ChooseLongSupplier;
import io.kroxylicious.filter.record.manipulation.common.OpFactory;
import io.kroxylicious.filter.record.manipulation.common.TypedOp;
import io.kroxylicious.proxy.plugin.Plugin;

/**
 * Generates a random {@link Integer} drawn from a fixed set.
 */
@Plugin(configType = ChooseLong.Config.class)
public class ChooseLong implements OpFactory<Long, Long> {

    /**
     * Configuration for {@link ChooseLong}.
     * @param from the set of values to choose from
     */
    public record Config(List<Long> from) {}

    @Override
    public TypedOp<Long, Long> create(Map<String, Object> configMap) {
        Config config = OpConfigs.MAPPER.convertValue(configMap, Config.class);
        var generator = new ChooseLongSupplier(new HashSet<>(config.from()));
        return TypedOp.of(Long.class, (ignored, context) -> generator.applyAsLong(context));
    }
}

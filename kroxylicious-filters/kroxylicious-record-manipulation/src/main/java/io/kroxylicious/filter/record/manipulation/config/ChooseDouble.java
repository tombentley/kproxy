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
import io.kroxylicious.filter.record.manipulation.common.ChooseLongSupplier;
import io.kroxylicious.filter.record.manipulation.common.IntOp;
import io.kroxylicious.filter.record.manipulation.common.IntOpFactory;
import io.kroxylicious.filter.record.manipulation.common.LongOp;
import io.kroxylicious.filter.record.manipulation.common.LongOpFactory;
import io.kroxylicious.proxy.plugin.Plugin;

/**
 * Generates a random {@link Integer} drawn from a fixed set. See {@link ChooseString} for the equivalent
 * {@link String} operation.
 */
@Plugin(configType = ChooseLong.Config.class)
public class ChooseLong implements LongOpFactory {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    /**
     * Configuration for {@link ChooseLong}.
     * @param from the set of values to choose from
     */
    public record Config(List<Long> from) {}

    @Override
    public LongOp create(Map<String, Object> configMap) {
        Config config = MAPPER.convertValue(configMap, Config.class);
        var generator = new ChooseLongSupplier(new HashSet<>(config.from()));
        return (ignored, context) -> generator.applyAsLong(context);
    }
}

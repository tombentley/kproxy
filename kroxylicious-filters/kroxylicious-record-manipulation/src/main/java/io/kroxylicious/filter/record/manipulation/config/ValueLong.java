/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.record.manipulation.config;

import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.kroxylicious.filter.record.manipulation.common.LongOp;
import io.kroxylicious.filter.record.manipulation.common.LongOpFactory;
import io.kroxylicious.proxy.plugin.Plugin;

/**
 * Replaces a value with a fixed {@link Integer}. See {@link ValueString} for the equivalent
 * {@link String} operation.
 */
@Plugin(configType = ValueLong.Config.class)
public class ValueLong implements LongOpFactory {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    /**
     * Configuration for {@link ValueLong}.
     * @param value the constant value to use
     */
    public record Config(long value) {}

    @Override
    public LongOp create(Map<String, Object> configMap) {
        Config config = MAPPER.convertValue(configMap, Config.class);
        return (ignored, context) -> config.value();
    }
}

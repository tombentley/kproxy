/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.record.manipulation.config;

import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.kroxylicious.filter.record.manipulation.common.HmacStringFunction;
import io.kroxylicious.filter.record.manipulation.common.StringOp;
import io.kroxylicious.filter.record.manipulation.common.StringOpFactory;
import io.kroxylicious.proxy.plugin.Plugin;

/**
 * Replaces a {@link String} value with its HMAC. String-only: there is no numeric equivalent.
 */
@Plugin(configType = HmacString.Config.class)
public class HmacString implements StringOpFactory {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    /**
     * Configuration for {@link HmacString}.
     * @param keyId identifies the key to use - not yet consumed, since key material is currently drawn
     *              from {@link io.kroxylicious.filter.record.manipulation.common.Context} rather than
     *              looked up by ID (see the module README's "Key management" section)
     */
    public record Config(String keyId) {}

    @Override
    public StringOp create(Map<String, Object> configMap) {
        MAPPER.convertValue(configMap, Config.class);
        var fn = new HmacStringFunction();
        return (value, context) -> value == null ? null : fn.apply(value, context);
    }
}

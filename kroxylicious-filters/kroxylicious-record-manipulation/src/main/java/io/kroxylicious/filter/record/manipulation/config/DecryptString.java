/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.record.manipulation.config;

import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.kroxylicious.filter.record.manipulation.common.DecryptStringFunction;
import io.kroxylicious.filter.record.manipulation.common.OpFactory;
import io.kroxylicious.filter.record.manipulation.common.TypedOp;
import io.kroxylicious.proxy.plugin.Plugin;

/**
 * Decrypts a {@link String} value. String-only: there is no numeric equivalent. See {@link EncryptString}
 * for the inverse operation.
 */
@Plugin(configType = DecryptString.Config.class)
public class DecryptString implements OpFactory<String, String> {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    /**
     * Configuration for {@link DecryptString}.
     * @param keyId identifies the key to use - not yet consumed, since key material is currently drawn
     *              from {@link io.kroxylicious.filter.record.manipulation.common.Context} rather than
     *              looked up by ID (see the module README's "Key management" section)
     */
    public record Config(String keyId) {}

    @Override
    public TypedOp<String, String> create(Map<String, Object> configMap) {
        MAPPER.convertValue(configMap, Config.class);
        var fn = new DecryptStringFunction();
        return TypedOp.of(String.class, (value, context) -> value == null ? null : fn.apply(value, context));
    }
}

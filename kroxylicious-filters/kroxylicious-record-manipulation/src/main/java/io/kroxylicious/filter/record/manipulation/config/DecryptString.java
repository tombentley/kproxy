/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.record.manipulation.config;

import java.lang.reflect.Type;
import java.util.Map;

import io.kroxylicious.filter.record.manipulation.op.BaseTypedOp;
import io.kroxylicious.filter.record.manipulation.common.DecryptStringFunction;
import io.kroxylicious.filter.record.manipulation.op.OpContext;
import io.kroxylicious.filter.record.manipulation.op.OpFactory;
import io.kroxylicious.filter.record.manipulation.common.PluginLookup;
import io.kroxylicious.filter.record.manipulation.common.StaticTypedOp;
import io.kroxylicious.proxy.plugin.Plugin;

/**
 * Decrypts a {@link String} value. String-only: there is no numeric equivalent. See {@link EncryptString}
 * for the inverse operation.
 */
@Plugin(configType = DecryptString.Config.class)
public class DecryptString implements OpFactory<String, String> {

    /**
     * Configuration for {@link DecryptString}.
     * @param keyId identifies the key to use - not yet consumed, since key material is currently drawn
     *              from {@link OpContext} rather than
     *              looked up by ID (see the module README's "Key management" section)
     */
    public record Config(String keyId) {}

    @Override
    public BaseTypedOp<String, String> create(Map<String, Object> configMap, PluginLookup lookup, Type argumentType) {
        OpConfigs.OP_CONFIG_MAPPER.convertValue(configMap, Config.class);
        var fn = new DecryptStringFunction();
        return new StaticTypedOp<String, String>() {
            @Override
            public Type outputType(Type inputType) {
                return String.class;
            }

            @Override
            public String apply(String value, OpContext opContext) {
                return value == null ? null : fn.apply(value, opContext);
            }
        };
    }
}

/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.record.manipulation.ops.encryption;

import java.lang.reflect.Type;
import java.util.Map;

import io.kroxylicious.filter.record.manipulation.common.PluginLookup;
import io.kroxylicious.filter.record.manipulation.config.OpConfigs;
import io.kroxylicious.filter.record.manipulation.op.BaseTypedOp;
import io.kroxylicious.filter.record.manipulation.op.OpContext;
import io.kroxylicious.filter.record.manipulation.op.OpFactory;
import io.kroxylicious.proxy.plugin.Plugin;

/**
 * Encrypts a {@link String} value. String-only: there is no numeric equivalent. See {@link DecryptString}
 * for the inverse operation.
 */
@Plugin(configType = EncryptString.Config.class)
public class EncryptString implements OpFactory<String, String> {

    /**
     * Configuration for {@link EncryptString}.
     * @param keyId identifies the key to use - not yet consumed, since key material is currently drawn
     *              from {@link OpContext} rather than
     *              looked up by ID (see the module README's "Key management" section)
     */
    public record Config(String keyId) {}

    @Override
    public BaseTypedOp<String, String> create(Map<String, Object> configMap, PluginLookup lookup, Type argumentType) {
        OpConfigs.OP_CONFIG_MAPPER.convertValue(configMap, Config.class);
        var fn = new EncryptStringFunction();
        return BaseTypedOp.of(String.class, String.class, (String value, OpContext opContext) -> value == null ? null : fn.apply(value, opContext));
    }
}

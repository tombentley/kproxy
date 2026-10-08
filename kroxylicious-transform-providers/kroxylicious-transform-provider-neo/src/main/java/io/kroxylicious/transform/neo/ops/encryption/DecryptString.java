/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.transform.neo.ops.encryption;

import java.lang.reflect.Type;
import java.util.Map;

import io.kroxylicious.proxy.plugin.Plugin;
import io.kroxylicious.transform.neo.op.BaseTypedOp;
import io.kroxylicious.transform.neo.op.OpContext;
import io.kroxylicious.transform.neo.op.OpFactory;
import io.kroxylicious.transform.service.TransformInitializationContext;

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
    public BaseTypedOp<String, String> create(Map<String, Object> configMap, TransformInitializationContext lookup, Type argumentType) {
        Mapper.OP_CONFIG_MAPPER.convertValue(configMap, Config.class);
        var fn = new DecryptStringFunction();
        return BaseTypedOp.of(String.class, String.class, (String value, OpContext opContext) -> value == null ? null : fn.apply(value, opContext));
    }
}

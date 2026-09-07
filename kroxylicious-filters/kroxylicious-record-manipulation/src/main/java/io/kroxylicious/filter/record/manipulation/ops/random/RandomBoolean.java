/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.record.manipulation.ops.random;

import java.lang.reflect.Type;
import java.util.Map;

import io.kroxylicious.filter.record.manipulation.common.PluginLookup;
import io.kroxylicious.filter.record.manipulation.common.StaticTypedOp;
import io.kroxylicious.filter.record.manipulation.op.BaseTypedOp;
import io.kroxylicious.filter.record.manipulation.op.OpContext;
import io.kroxylicious.filter.record.manipulation.op.OpFactory;
import io.kroxylicious.proxy.plugin.Plugin;

/**
 * Generates a random {@link Integer} drawn from a range.
 */
@Plugin(configType = RandomBoolean.Config.class)
public class RandomBoolean implements OpFactory<Boolean, Boolean> {

    /**
     * Configuration for {@link RandomBoolean}.
     */
    public record Config() {}

    @Override
    public BaseTypedOp<Boolean, Boolean> create(Map<String, Object> configMap, PluginLookup lookup, Type argumentType) {
        var generator = new RandomBooleanSupplier();
        return new StaticTypedOp<Boolean, Boolean>() {
            @Override
            public Type outputType(Type inputType) {
                return Boolean.class;
            }

            @Override
            public Boolean apply(Boolean value, OpContext opContext) {
                return generator.test(opContext);
            }
        };
    }
}

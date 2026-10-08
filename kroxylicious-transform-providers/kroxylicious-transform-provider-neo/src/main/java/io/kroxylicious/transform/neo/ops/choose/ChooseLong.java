/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.transform.neo.ops.choose;

import java.lang.reflect.Type;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import io.kroxylicious.proxy.plugin.Plugin;
import io.kroxylicious.transform.neo.op.BaseTypedOp;
import io.kroxylicious.transform.neo.op.OpFactory;
import io.kroxylicious.transform.service.TransformInitializationContext;

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
    public BaseTypedOp<Long, Long> create(Map<String, Object> configMap,
                                          TransformInitializationContext lookup,
                                          Type argumentType) {
        Config config = Mapper.OP_CONFIG_MAPPER.convertValue(configMap, Config.class);
        var generator = new ChooseLongSupplier(new HashSet<>(config.from()));
        return BaseTypedOp.of(Long.class, Long.class, (value, opContext) -> generator.applyAsLong(opContext));
    }
}

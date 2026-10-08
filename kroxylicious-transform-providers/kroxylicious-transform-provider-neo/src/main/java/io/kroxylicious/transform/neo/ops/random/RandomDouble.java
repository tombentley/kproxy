/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.transform.neo.ops.random;

import java.lang.reflect.Type;
import java.util.Map;

import io.kroxylicious.proxy.plugin.Plugin;
import io.kroxylicious.transform.neo.op.BaseTypedOp;
import io.kroxylicious.transform.neo.op.OpFactory;
import io.kroxylicious.transform.service.TransformInitializationContext;

/**
 * Generates a random {@link Double} drawn from a range.
 */
@Plugin(configType = RandomDouble.Config.class)
public class RandomDouble implements OpFactory<Double, Double> {

    /**
     * Configuration for {@link RandomDouble}.
     * @param minInclusive the minimum value (inclusive)
     * @param maxExclusive the maximum value (exclusive)
     */
    public record Config(double minInclusive, double maxExclusive) {}

    @Override
    public BaseTypedOp<Double, Double> create(Map<String, Object> configMap, TransformInitializationContext lookup, Type argumentType) {
        Config config = Mapper.OP_CONFIG_MAPPER.convertValue(configMap, Config.class);
        var generator = new RandomDoubleSupplier(config.minInclusive(), config.maxExclusive());
        return BaseTypedOp.of(Double.class, Double.class, (value, opContext) -> generator.applyAsDouble(opContext));
    }
}

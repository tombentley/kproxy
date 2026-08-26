/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.record.manipulation.config;

import java.math.BigInteger;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.kroxylicious.filter.record.manipulation.common.BigIntegerOp;
import io.kroxylicious.filter.record.manipulation.common.BigIntegerOpFactory;
import io.kroxylicious.filter.record.manipulation.common.ChooseBigIntegerSupplier;
import io.kroxylicious.proxy.plugin.Plugin;

/**
 * Generates a random {@link BigInteger} drawn from a fixed set.
 */
@Plugin(configType = ChooseBigInteger.Config.class)
public class ChooseBigInteger implements BigIntegerOpFactory {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    /**
     * Configuration for {@link ChooseBigInteger}.
     * @param from the set of values to choose from
     */
    public record Config(List<BigInteger> from) {}

    @Override
    public BigIntegerOp create(Map<String, Object> configMap) {
        Config config = MAPPER.convertValue(configMap, Config.class);
        var generator = new ChooseBigIntegerSupplier(new HashSet<>(config.from()));
        return (ignored, context) -> generator.apply(context);
    }
}

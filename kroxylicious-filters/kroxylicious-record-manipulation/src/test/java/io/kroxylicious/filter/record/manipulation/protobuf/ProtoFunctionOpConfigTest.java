/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.record.manipulation.protobuf;

import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.function.BiFunction;
import java.util.stream.IntStream;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.kroxylicious.filter.record.manipulation.common.Context;
import io.kroxylicious.filter.record.manipulation.common.ContextPipeline;
import io.kroxylicious.filter.record.manipulation.common.IntOp;
import io.kroxylicious.filter.record.manipulation.common.PluginLookup;
import io.kroxylicious.filter.record.manipulation.config.OpConfig;
import io.kroxylicious.proxy.config.ServiceBasedPluginFactoryRegistry;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Proves {@link ProtoFunction#buildIntegerOp(OpConfig, PluginLookup)} resolves a pluggable operation by
 * name and produces a working, composable {@link IntOp} - not yet reachable from
 * {@link ProtoFunction#buildStringOp}/{@code buildApplyChain}'s {@link
 * io.kroxylicious.filter.record.manipulation.config.ApplyConfig}-driven path.
 */
class ProtoFunctionOpConfigTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private static final PluginLookup LOOKUP = new PluginLookup() {
        private final ServiceBasedPluginFactoryRegistry registry = new ServiceBasedPluginFactoryRegistry();

        @Override
        public <P> P pluginInstance(Class<P> pluginClass, String implementationName) {
            return registry.pluginFactory(pluginClass).pluginInstance(implementationName);
        }
    };

    private static Context contextWithSeed(long seed) {
        return new Context(new Random(seed), new byte[0]);
    }

    @Test
    void resolvesRandomIntByNameAndProducesValuesInRange() {
        // Given
        OpConfig op = new OpConfig("RandomInt", Map.of("minInclusive", 10, "maxExclusive", 20));
        IntOp built = ProtoFunction.buildIntegerOp(op, LOOKUP);
        Context context = contextWithSeed(0);

        // When
        int[] values = IntStream.range(0, 500).map(i -> built.apply(0, context)).toArray();

        // Then
        assertThat(IntStream.of(values).allMatch(value -> value >= 10 && value < 20)).isTrue();
    }

    @Test
    void resolvedOpComposesWithAnotherIntOpViaContextPipeline() {
        // Given
        OpConfig op = new OpConfig("RandomInt", Map.of("minInclusive", 5, "maxExclusive", 6));
        IntOp built = ProtoFunction.buildIntegerOp(op, LOOKUP);
        IntOp addOne = (value, context) -> value + 1;
        ContextPipeline pipeline = new ContextPipeline(List.<BiFunction<?, Context, ?>> of(built, addOne));

        // When
        int result = pipeline.apply(0, contextWithSeed(0));

        // Then
        assertThat(result).isEqualTo(6);
    }

    @Test
    void resolvesAnOpConfigDeserializedFromASquashedJsonObject() throws Exception {
        // Given
        OpConfig op = MAPPER.readValue("""
                {"op": "RandomInt", "minInclusive": 10, "maxExclusive": 20}
                """, OpConfig.class);
        IntOp built = ProtoFunction.buildIntegerOp(op, LOOKUP);
        Context context = contextWithSeed(0);

        // When
        int[] values = IntStream.range(0, 500).map(i -> built.apply(0, context)).toArray();

        // Then
        assertThat(IntStream.of(values).allMatch(value -> value >= 10 && value < 20)).isTrue();
    }

}

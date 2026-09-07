/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.record.manipulation.format.protobuf;

import java.util.Random;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.kroxylicious.filter.record.manipulation.op.OpContext;
import io.kroxylicious.filter.record.manipulation.common.PluginLookup;
import io.kroxylicious.proxy.config.ServiceBasedPluginFactoryRegistry;

class ProtobufFunctionOpConfigTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private static final PluginLookup LOOKUP = new PluginLookup() {
        private final ServiceBasedPluginFactoryRegistry registry = new ServiceBasedPluginFactoryRegistry();

        @Override
        public <P> P pluginInstance(Class<P> pluginClass, String implementationName) {
            return registry.pluginFactory(pluginClass).pluginInstance(implementationName);
        }
    };

    private static OpContext contextWithSeed(long seed) {
        return new OpContext(new Random(seed), new byte[0]);
    }

//    @Test
//    void resolvesRandomIntByNameAndProducesValuesInRange() {
//        // Given
//        OpConfig op = new OpConfig("RandomInt", Map.of("minInclusive", 10, "maxExclusive", 20));
//        TypedOp<Integer, Integer> built = ProtobufFunction.buildOp(op, Integer.class, Integer.class, LOOKUP);
//        OpContext opContext = contextWithSeed(0);
//
//        // When
//        int[] values = IntStream.range(0, 500).map(i -> built.apply(0, opContext)).toArray();
//
//        // Then
//        assertThat(IntStream.of(values).allMatch(value -> value >= 10 && value < 20)).isTrue();
//    }

//    @Test
//    void resolvedOpComposesWithAnotherIntOpViaContextPipeline() {
//        // Given
//        OpConfig op = new OpConfig("RandomInt", Map.of("minInclusive", 5, "maxExclusive", 6));
//        TypedOp<Integer, Integer> built = ProtobufFunction.buildOp(op, Integer.class, Integer.class, LOOKUP);
//        TypedOp<Integer, Integer> addOne = new TypedOp<Integer, Integer>() {
//            @Override
//            public Integer apply(Integer value, OpContext opContext) {
//                return value + 1;
//            }
//        };
//        OpPipeline<Integer, Integer> pipeline = new OpPipeline<>(List.<TypedOp<?, ?>> of(built, addOne));
//
//        // When
//        int result = pipeline.apply(0, contextWithSeed(0));
//
//        // Then
//        assertThat(result).isEqualTo(6);
//    }

//    @Test
//    void resolvesAnOpConfigDeserializedFromASquashedJsonObject() throws Exception {
//        // Given
//        OpConfig op = MAPPER.readValue("""
//                {"op": "RandomInt", "minInclusive": 10, "maxExclusive": 20}
//                """, OpConfig.class);
//        BaseTypedOp<Integer, Integer> built = ProtobufFunction.buildOp(op, Integer.class, Integer.class, LOOKUP);
//        OpContext opContext = contextWithSeed(0);
//
//        // When
//        int[] values = IntStream.range(0, 500).map(i -> built.apply(0, opContext)).toArray();
//
//        // Then
//        assertThat(IntStream.of(values).allMatch(value -> value >= 10 && value < 20)).isTrue();
//    }

}

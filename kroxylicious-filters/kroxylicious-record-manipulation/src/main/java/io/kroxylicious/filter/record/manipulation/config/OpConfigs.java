/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.record.manipulation.config;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.leangen.geantyref.TypeToken;

import io.kroxylicious.filter.record.manipulation.common.OpFactory;
import io.kroxylicious.filter.record.manipulation.common.PluginLookup;
import io.kroxylicious.filter.record.manipulation.common.TypedOp;

/**
 * Resolves an {@link OpConfig} to a built operation via {@link PluginLookup} - the part of building an
 * {@code apply} chain that's identical across every format engine (Jackson/Avro/Protobuf), so it lives
 * here once rather than being duplicated in each engine's own {@code buildOp}.
 * <p>
 * Deliberately does not know about {@link #DELETE}: whether that name is legal at all is a property of the
 * calling format's container model (can it represent "this property is absent"?), not something a shared,
 * name-keyed plugin registry can answer - see each engine's own {@code buildOp} for how they handle it
 * before ever calling here.
 */
public final class OpConfigs {

    /**
     * The reserved, non-pluggable op name every format engine special-cases itself, rather than resolving
     * through {@link PluginLookup} - see the class javadoc for why.
     */
    public static final String DELETE = "Delete";
    static final ObjectMapper MAPPER = new ObjectMapper();

    private OpConfigs() {
    }

    /**
     * Resolves {@code op} to an {@link OpFactory} and builds its operation, checking that the built
     * operation's declared input/output types match what the caller expected.
     * @param op the operation to resolve; must not be {@link #DELETE}
     * @param inputType the input type the built operation must have
     * @param outputType the output type the built operation must have
     * @param lookup the plugin lookup to resolve {@code op}'s name against
     * @return the built operation
     * @throws IllegalArgumentException if the resolved operation's input/output types don't match
     * @param <T> the input type
     * @param <R> the output type
     */
    @SuppressWarnings("unchecked")
    public static <T, R> TypedOp<T, R> resolveOp(OpConfig op, TypeToken<T> inputType, TypeToken<R> outputType, PluginLookup lookup) {
        OpFactory<?, ?> factory = lookup.pluginInstance(OpFactory.class, op.op());
        TypedOp<?, ?> built = factory.create(op.config());
        if (!built.inputType().equals(inputType) || !built.outputType().equals(outputType)) {
            throw new IllegalArgumentException("Operation '" + op.op() + "' produces "
                    + built.inputType().getType() + "->" + built.outputType().getType()
                    + ", but " + inputType.getType() + "->" + outputType.getType() + " was required");
        }
        return (TypedOp<T, R>) built;
    }

    /**
     * The {@link Class}-keyed counterpart of {@link #resolveOp(OpConfig, TypeToken, TypeToken, PluginLookup)},
     * for the common case of a plain, non-parameterized input/output type.
     * @param op the operation to resolve; must not be {@link #DELETE}
     * @param inputType the input type the built operation must have
     * @param outputType the output type the built operation must have
     * @param lookup the plugin lookup to resolve {@code op}'s name against
     * @return the built operation
     * @throws IllegalArgumentException if the resolved operation's input/output types don't match
     * @param <T> the input type
     * @param <R> the output type
     */
    public static <T, R> TypedOp<T, R> resolveOp(OpConfig op, Class<T> inputType, Class<R> outputType, PluginLookup lookup) {
        return resolveOp(op, TypeToken.get(inputType), TypeToken.get(outputType), lookup);
    }
}

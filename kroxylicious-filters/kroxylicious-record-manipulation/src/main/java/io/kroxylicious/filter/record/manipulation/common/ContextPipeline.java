/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.record.manipulation.common;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Set;
import java.util.function.BiFunction;

import io.leangen.geantyref.GenericTypeReflector;

/**
 * Validates and composes a list of {@link BiFunction}s that each transform a value given some
 * {@link Context}, threading the same {@code Context} through every stage. A sibling to {@link Pipeline},
 * not a generalisation of it: {@link Pipeline} composes whole-record processing stages that never need a
 * {@link Context} (e.g. deserialize/serialize); this composes a single field's {@code apply} chain, where
 * every stage may need one.
 * <p>
 * Always validates that the return type of each function is assignable to the value-parameter type of the
 * next (exactly like {@link Pipeline}'s equivalent check), reflecting on each stage's <em>concrete</em>
 * generic type - which is why chain elements need to be named classes/interfaces with fixed type arguments
 * (see {@link StringOp}/{@link IntOp}), not bare lambdas. Additionally verifies any requested
 * {@link Requirement} against the composed chain.
 */
public class ContextPipeline {

    private final List<BiFunction<?, Context, ?>> functions;

    /**
     * Validates and creates a pipeline with no additional requirements beyond adjacent stages composing.
     * @param functions the functions to validate, in composition order
     * @throws RuntimeException if consecutive functions do not compose
     */
    public ContextPipeline(List<BiFunction<?, Context, ?>> functions) {
        this(functions, Set.of());
    }

    /**
     * Validates and creates a pipeline.
     * @param functions the functions to validate, in composition order
     * @param requirements additional properties to verify of the composed chain
     * @throws RuntimeException if consecutive functions do not compose, or a requested requirement isn't met
     */
    public ContextPipeline(List<BiFunction<?, Context, ?>> functions, Set<Requirement> requirements) {
        for (int i = 1; i < functions.size(); i++) {
            BiFunction<?, Context, ?> last = functions.get(i - 1);
            BiFunction<?, Context, ?> next = functions.get(i);
            Type returnType = functionReturnType(last);
            Type parameterType = functionParameterType(next);
            if (!GenericTypeReflector.isSuperType(parameterType, returnType)) {
                throw new RuntimeException("ContextPipeline functions do not compose: "
                        + "function at index " + (i - 1) + " has return type " + returnType
                        + " which is not assignable to " + parameterType
                        + ", the parameter type of function at index " + i);
            }
        }
        if (requirements.contains(Requirement.TYPE_PRESERVING) && !functions.isEmpty()) {
            Type inputType = functionParameterType(functions.get(0));
            Type outputType = functionReturnType(functions.get(functions.size() - 1));
            if (!inputType.equals(outputType)) {
                throw new RuntimeException("ContextPipeline is not type-preserving: "
                        + "input type " + inputType + " does not equal output type " + outputType);
            }
        }
        this.functions = functions;
    }

    /**
     * Runs the pipeline, feeding {@code input} and {@code context} to the first function, and the result of
     * each function (with the same {@code context}) to the next.
     * @param input the input to the first function
     * @param context the context threaded through every function
     * @return the result of the last function, or {@code input} itself if the pipeline is empty
     * @param <T> the input type
     * @param <R> the result type
     */
    @SuppressWarnings("unchecked")
    public <T, R> R apply(T input, Context context) {
        Object result = input;
        for (BiFunction<?, Context, ?> function : functions) {
            result = ((BiFunction<Object, Context, Object>) function).apply(result, context);
        }
        return (R) result;
    }

    private static Type[] functionTypeArguments(BiFunction<?, Context, ?> function) {
        Type functionType = GenericTypeReflector.getExactSuperType(function.getClass(), BiFunction.class);
        if (functionType instanceof ParameterizedType pt
                && pt.getRawType().equals(BiFunction.class)) {
            return pt.getActualTypeArguments();
        }
        else {
            throw new RuntimeException("Could not find BiFunction supertype of " + functionType);
        }
    }

    private static Type functionParameterType(BiFunction<?, Context, ?> function) {
        return functionTypeArguments(function)[0];
    }

    private static Type functionReturnType(BiFunction<?, Context, ?> function) {
        return functionTypeArguments(function)[2];
    }
}

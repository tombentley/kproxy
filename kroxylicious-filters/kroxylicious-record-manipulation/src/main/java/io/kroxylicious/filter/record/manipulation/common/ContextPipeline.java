/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.record.manipulation.common;

import java.util.List;
import java.util.Set;
import java.util.function.BiFunction;

import io.leangen.geantyref.GenericTypeReflector;

/**
 * Validates and composes a list of {@link TypedOp}s that each transform a value given some
 * {@link Context}, threading the same {@code Context} through every stage. A sibling to {@link Pipeline},
 * not a generalisation of it: {@link Pipeline} composes whole-record processing stages that never need a
 * {@link Context} (e.g. deserialize/serialize); this composes a single field's {@code apply} chain, where
 * every stage may need one.
 * <p>
 * Always validates that the return type of each function is assignable to the value-parameter type of the
 * next (exactly like {@link Pipeline}'s equivalent check, reusing the same
 * {@link GenericTypeReflector#isSuperType(java.lang.reflect.Type, java.lang.reflect.Type)}), except the
 * types being compared come from each {@link TypedOp}'s own {@link TypedOp#inputType()}/
 * {@link TypedOp#outputType()} rather than from reflecting on a lambda's declared interface - which is
 * why a chain element only needs to be a {@link TypedOp}, not a named class/interface with fixed type
 * arguments. Additionally verifies any requested {@link Requirement} against the composed chain.
 * @param <T> the input type
 * @param <R> the result type
 */
public class ContextPipeline<T, R> implements BiFunction<T, Context, R> {

    private final List<TypedOp<?, ?>> functions;

    /**
     * Validates and creates a pipeline with no additional requirements beyond adjacent stages composing.
     * @param functions the functions to validate, in composition order
     * @throws RuntimeException if consecutive functions do not compose
     */
    public ContextPipeline(List<TypedOp<?, ?>> functions) {
        this(functions, Set.of());
    }

    /**
     * Validates and creates a pipeline.
     * @param functions the functions to validate, in composition order
     * @param requirements additional properties to verify of the composed chain
     * @throws RuntimeException if consecutive functions do not compose, or a requested requirement isn't met
     */
    public ContextPipeline(List<TypedOp<?, ?>> functions, Set<Requirement> requirements) {
        for (int i = 1; i < functions.size(); i++) {
            TypedOp<?, ?> last = functions.get(i - 1);
            TypedOp<?, ?> next = functions.get(i);
            if (!GenericTypeReflector.isSuperType(next.inputType().getType(), last.outputType().getType())) {
                throw new RuntimeException("ContextPipeline functions do not compose: "
                        + "function at index " + (i - 1) + " has return type " + last.outputType().getType()
                        + " which is not assignable to " + next.inputType().getType()
                        + ", the parameter type of function at index " + i);
            }
        }
        if (requirements.contains(Requirement.TYPE_PRESERVING) && !functions.isEmpty()) {
            var inputType = functions.get(0).inputType();
            var outputType = functions.get(functions.size() - 1).outputType();
            if (!inputType.equals(outputType)) {
                throw new RuntimeException("ContextPipeline is not type-preserving: "
                        + "input type " + inputType.getType() + " does not equal output type " + outputType.getType());
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
     */
    @SuppressWarnings("unchecked")
    @Override
    public R apply(T input, Context context) {
        Object result = input;
        for (TypedOp<?, ?> function : functions) {
            result = ((TypedOp<Object, Object>) function).apply(result, context);
        }
        return (R) result;
    }
}

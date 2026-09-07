/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.record.manipulation.common;

import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;

import io.leangen.geantyref.GenericTypeReflector;

import edu.umd.cs.findbugs.annotations.UnknownNullness;

/**
 * Conceptually a {@code BiFunction<T, OpContext, R>} which provides
 * accessors for the {@link Type Types} reflecting {@code T} and {@code R}.
 *
 * Many implementations of this interface are types with statically
 * type arguments (i.e. {@code T} and {@code R} are statically known types,
 * and so their {@code Type} reflections are of type
 * {@link Class} or {@link java.lang.reflect.ParameterizedType}).
 * Such implementations can subclass {@link StaticTypedOp}, which captures
 * the static types given in the {@code extends} clause.
 *
 * There are other BaseTypedOps which do not have statically known types.
 * {@link ComposedOp} represents the composition of two other operations.
 * {@link IdentityOp} represents the identity operation, which always returns
 * the {@code T} it is given.
 * @param <T> The parameter type
 * @param <R> The return type
 */
public interface BaseTypedOp<T, R> {

    /**
     * Create a {@code BaseTypedOp} by explicitly passing its input and output types.
     * This factory function can accept a lamba as {@code fn}, but does not check that the
     * static type of the {@code fn} is compatible with the input and output types
     * @param inputType The type reflecting {@code T}
     * @param outputType The type reflecting {@code R}
     * @param fn The function to apply.
     * @return A BaseTypedOp
     * @param <T> The input type
     * @param <R> The output type
     */
    static <T, R> BaseTypedOp<T, R> of(Type inputType, Type outputType, BiFunction<T, OpContext, R> fn) {
        return new BaseTypedOp<T, R>() {
            @Override
            public Type inputType() {
                return inputType;
            }

            @Override
            public Type outputType() {
                return outputType;
            }

            @Override
            public R apply(T value, OpContext opContext) {
                return fn.apply(value, opContext);
            }
        };
    }

    /**
     * The {@code Type} reflecting there first parameter, {@code T}.
     * @return The type reflecting {@code T}
     */
    Type inputType();

    /**
     * The {@code Type} reflecting the result type, {@code R}.
     * @return The type reflecting {@code R}
     */
    Type outputType();

    default List<? extends TypeVariable<? extends Class<?>>> typeParameters() {
        return List.of();
    }

    default Type typeApply(List<Type> typeArguments) {
        // What does this method return? a Type?
        if (typeArguments.size() != typeParameters().size()) {
            String prefix;
            if (typeArguments.size() < typeParameters().size()) {
                prefix = "Too few type arguments given: there are ";
            }
            else {
                prefix = "Too many type arguments given: there are ";
            }
            throw new IllegalArgumentException(prefix + typeParameters().size() + " parameters, but " + typeArguments.size() + " arguments were given");
        }

        if (typeParameters().isEmpty()) {
            // not generic
            if (!GenericTypeReflector.isFullyBound(outputType())) {
                throw new TypeException();
            }
            return outputType();
        }

        Map<TypeVariable<? extends Class<?>>, Type> substitutions = new HashMap<>();
        for (int i = 0; i < typeArguments.size(); i++) {
            substitutions.put(typeParameters().get(i), typeArguments.get(i));
        }

        return null;
//        return typeParameters().stream().map(typeParameter -> {
//            var typeArgument = substitutions.get(typeParameter);
//            for (var bound : typeParameter.getBounds()) {
//                if (!GenericTypeReflector.isSuperType(bound, typeArgument)) {
//                    throw new TypeException("Type argument " + typeArgument + " is within bound " + bound);
//                }
//            }
//            return typeArgument;
//        });
    }

    @UnknownNullness
    R apply(T value, OpContext opContext);


}

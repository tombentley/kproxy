/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.record.manipulation.op;

import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.List;
import java.util.function.BiFunction;

import io.kroxylicious.filter.record.manipulation.common.ComposedOp;
import io.kroxylicious.filter.record.manipulation.common.IdentityOp;
import io.kroxylicious.filter.record.manipulation.common.StaticTypedOp;

import edu.umd.cs.findbugs.annotations.UnknownNullness;

/**
 * Conceptually a {@code BiFunction<T, OpContext, R>} which provides
 * accessors for the {@link Type Types} reflecting {@code T} and {@code R}.
 *
 * Most implementations have statically known, non-parameterized {@code T}/{@code R} (e.g.
 * {@code String}, {@code Integer}, {@code ByteBuffer}) and should use {@link #of(Class, Class, BiFunction)}.
 * Only when {@code T} or {@code R} is a genuinely parameterized type (e.g. {@code Maybe<JsonNode>}) that
 * can't be expressed as a bare {@link Class} literal is {@link StaticTypedOp} needed instead - it captures
 * the static types given in its {@code extends} clause via reflection, at the cost of requiring a named or
 * anonymous class rather than a lambda.
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
     * static type of the {@code fn} is compatible with the input and output types.
     * Prefer {@link #of(Class, Class, BiFunction)} when {@code T}/{@code R} are statically known,
     * non-parameterized types - this overload exists for cases where the input/output types are only
     * known at runtime (e.g. derived by reflection from a caller-supplied {@link Type}).
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
     * Create a {@code BaseTypedOp} from a pair of {@link Class} literals and a lambda. Since {@code T}/{@code R}
     * are tied to both the {@code Class} arguments and {@code fn}'s actual parameter/return types, a mismatch
     * between the declared types and {@code fn}'s real signature is a compile error - this is the
     * recommended way to build a {@code BaseTypedOp} whenever {@code T}/{@code R} are statically known,
     * non-parameterized types.
     * @param inputType The class reflecting {@code T}
     * @param outputType The class reflecting {@code R}
     * @param fn The function to apply.
     * @return A BaseTypedOp
     * @param <T> The input type
     * @param <R> The output type
     */
    static <T, R> BaseTypedOp<T, R> of(Class<T> inputType, Class<R> outputType, BiFunction<T, OpContext, R> fn) {
        return of((Type) inputType, (Type) outputType, fn);
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

    @UnknownNullness
    R apply(T value, OpContext opContext);

}

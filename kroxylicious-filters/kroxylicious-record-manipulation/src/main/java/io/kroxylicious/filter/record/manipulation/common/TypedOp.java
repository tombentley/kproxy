/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.record.manipulation.common;

import java.util.function.BiFunction;

import io.leangen.geantyref.TypeToken;

/**
 * An operation on a value of type {@code T}, given some {@link Context}, producing a value of type
 * {@code R} - paired with its own input/output {@link TypeToken}s, carried as data rather than left to
 * be recovered by reflecting on a lambda's declared type. A lambda assigned directly to a generic
 * {@code BiFunction<T, Context, R>} erases its type arguments at runtime, so without an explicit token
 * a caller composing a chain of these (see {@link ContextPipeline}) would have no way to check, at
 * build time, that one stage's output is fit to feed the next.
 * <p>
 * {@link TypeToken}, not {@link Class}, is the token type: a {@code Class} erases its own generic
 * parameters (a {@code List<Integer>} and a {@code List<String>} are both just {@code List.class}),
 * whereas a {@code TypeToken} wraps a full {@link java.lang.reflect.Type} and compares accordingly. The
 * {@link #of(Class, Class, BiFunction)}/{@link #of(Class, BiFunction)} overloads keep today's plain base
 * types (e.g. {@code String}, {@code Integer}) exactly as terse to declare as a {@code Class}-keyed
 * design would have been.
 * @param <T> the input type
 * @param <R> the output type
 */
public final class TypedOp<T, R> implements BiFunction<T, Context, R> {

    private final TypeToken<T> inputType;
    private final TypeToken<R> outputType;
    private final BiFunction<T, Context, R> delegate;

    private TypedOp(TypeToken<T> inputType, TypeToken<R> outputType, BiFunction<T, Context, R> delegate) {
        this.inputType = inputType;
        this.outputType = outputType;
        this.delegate = delegate;
    }

    /**
     * Creates an operation with explicit input/output {@link TypeToken}s, for a type not expressible as
     * a plain {@link Class} (e.g. a future operation over a parameterized type like {@code List<Integer>}).
     * @param inputType the input type
     * @param outputType the output type
     * @param delegate the operation itself
     * @return the created operation
     * @param <T> the input type
     * @param <R> the output type
     */
    public static <T, R> TypedOp<T, R> of(TypeToken<T> inputType, TypeToken<R> outputType, BiFunction<T, Context, R> delegate) {
        return new TypedOp<>(inputType, outputType, delegate);
    }

    /**
     * Creates an operation over plain, non-parameterized input/output types.
     * @param inputType the input type
     * @param outputType the output type
     * @param delegate the operation itself
     * @return the created operation
     * @param <T> the input type
     * @param <R> the output type
     */
    public static <T, R> TypedOp<T, R> of(Class<T> inputType, Class<R> outputType, BiFunction<T, Context, R> delegate) {
        return of(TypeToken.get(inputType), TypeToken.get(outputType), delegate);
    }

    /**
     * Creates a type-preserving operation over a plain, non-parameterized type.
     * @param type the input and output type
     * @param delegate the operation itself
     * @return the created operation
     * @param <T> the input and output type
     */
    public static <T> TypedOp<T, T> of(Class<T> type, BiFunction<T, Context, T> delegate) {
        return of(type, type, delegate);
    }

    /**
     * The input type of this operation.
     * @return the input type
     */
    public TypeToken<T> inputType() {
        return inputType;
    }

    /**
     * The output type of this operation.
     * @return the output type
     */
    public TypeToken<R> outputType() {
        return outputType;
    }

    @Override
    public R apply(T value, Context context) {
        return delegate.apply(value, context);
    }
}

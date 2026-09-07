/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.record.manipulation.common;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import io.leangen.geantyref.GenericTypeReflector;
import io.leangen.geantyref.TypeToken;

/**
 * An operation on a value of type {@code T}, given some {@link OpContext}, producing a value of type
 * {@code R}.
 * @param <T> the input type
 * @param <R> the output type
 */
public abstract class StaticTypedOp<T, R> implements BaseTypedOp<T, R> {

    protected StaticTypedOp() {
        if (!(GenericTypeReflector.getExactSuperType(TypeToken.get(getClass()).getType(), StaticTypedOp.class) instanceof ParameterizedType)) {
            throw new IllegalArgumentException();
        }
    }

    /**
     * The input type of this operation.
     * @return the input type
     */
    @Override
    public Type inputType() {
        Type exactSuperType = GenericTypeReflector.getExactSuperType(TypeToken.get(getClass()).getType(), StaticTypedOp.class);
        return ((ParameterizedType) exactSuperType).getActualTypeArguments()[0];
    }

    /**
     * The output type of this operation.
     * @return the output type
     */
    @Override
    public Type outputType() {
        Type exactSuperType = GenericTypeReflector.getExactSuperType(TypeToken.get(getClass()).getType(), StaticTypedOp.class);
        return ((ParameterizedType) exactSuperType).getActualTypeArguments()[1];
    }

    public abstract Type outputType(Type inputType);

}

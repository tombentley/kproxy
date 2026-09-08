/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.record.manipulation.common;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

import io.leangen.geantyref.GenericTypeReflector;
import io.leangen.geantyref.TypeToken;

import io.kroxylicious.filter.record.manipulation.op.BaseTypedOp;
import io.kroxylicious.filter.record.manipulation.op.OpContext;

/**
 * An operation on a value of type {@code T}, given some {@link OpContext}, producing a value of type
 * {@code R}, reifying {@code T}/{@code R} from the concrete subclass's {@code extends} clause.
 * <p>
 * This is only needed when {@code T} or {@code R} is a genuinely parameterized type (e.g.
 * {@code Maybe<JsonNode>}) that can't be expressed as a bare {@link Class} literal - for any other case,
 * prefer {@link BaseTypedOp#of(Class, Class, java.util.function.BiFunction)}, which is lambda-compatible
 * and needs no anonymous subclass.
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

}

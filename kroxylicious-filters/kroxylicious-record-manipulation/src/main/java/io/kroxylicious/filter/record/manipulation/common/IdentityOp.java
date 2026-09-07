/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.record.manipulation.common;

import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.List;

public class IdentityOp<T> implements BaseTypedOp<T, T> {

    private static IdentityOp INSTANCE = new IdentityOp();

    public static <T> BaseTypedOp<T, T> identity() {
        return INSTANCE;
    }

    private IdentityOp() {}

    @Override
    public List<? extends TypeVariable<? extends Class<?>>> typeParameters() {
        return List.of(getClass().getTypeParameters()[0]); // TypeVariable
    }

    @Override
    public Type inputType() {
        return getClass().getTypeParameters()[0]; // TypeVariable
    }

    @Override
    public Type outputType() {
        return getClass().getTypeParameters()[0]; // TypeVariable
    }

    @Override
    public T apply(T value, OpContext opContext) {
        return value;
    }
}

/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.record.manipulation.common;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Type;

import io.leangen.geantyref.GenericTypeReflector;

public class ComposedOp<T, R, S> implements BaseTypedOp<T, S> {

    private final BaseTypedOp<T, R> first;
    private final BaseTypedOp<R, S> then;

    public ComposedOp(BaseTypedOp<T, R> first, BaseTypedOp<R, S> then) {
        this.first = first;
        this.then = then;
        if (GenericTypeReflector.isFullyBound(first.outputType())
            && GenericTypeReflector.isFullyBound(then.inputType())) {
            if (!GenericTypeReflector.isSuperType(then.inputType(), first.outputType())) {
                throw new TypeException("Cannot compose " + first + " with " + then);
            }
        }
    }

    @Override
    public Type inputType() {
        // This means that we'd need a resolved type for `first` and `then` in order for this to have a resolved type
        return first.inputType();
    }

    @Override
    public Type outputType() {
        return then.outputType();
    }

    @Override
    public S apply(T value, OpContext opContext) {
        R apply = first.apply(value, opContext);
        return then.apply(apply, opContext);
    }

}

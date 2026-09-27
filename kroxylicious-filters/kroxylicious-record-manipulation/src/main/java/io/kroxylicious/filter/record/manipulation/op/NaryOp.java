/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.record.manipulation.op;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.List;

import edu.umd.cs.findbugs.annotations.UnknownNullness;

public interface NaryOp<R> {

    static <R> NaryOp<R> of(List<Type> inputTypes, Type outputType, MethodHandle h) {
        return new NaryOp<R>() {
            @Override
            public List<Type> inputTypes() {
                return inputTypes;
            }

            @Override
            public Type outputType() {
                return outputType;
            }

            @Override
            public R applyN(Object[] params, OpContext opContext) {
                try {
                    var f = MethodHandles.explicitCastArguments(h, MethodType.methodType((Class) outputType, inputTypes.stream().map(Class.class::cast).toArray(Class[]::new)));
                    return (R) f.invokeWithArguments(params);
                }
                catch (Throwable e) {
                    throw new RuntimeException(e);
                }
            }
        };
    }
    /**
     * The {@code Type} reflecting there first parameter, {@code T}.
     * @return The type reflecting {@code T}
     */
    List<Type> inputTypes();

    /**
     * The {@code Type} reflecting the result type, {@code R}.
     * @return The type reflecting {@code R}
     */
    Type outputType();

    default List<? extends TypeVariable<? extends Class<?>>> typeParameters() {
        return List.of();
    }

    @UnknownNullness
    R applyN(Object[] params, OpContext opContext);
}

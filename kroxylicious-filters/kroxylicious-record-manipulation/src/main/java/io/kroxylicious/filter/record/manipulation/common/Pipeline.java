/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.record.manipulation.common;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.List;
import java.util.function.Function;

import io.leangen.geantyref.GenericTypeReflector;

public class Pipeline {

    public Pipeline(List<Function<?, ?>> functions) {
        for (int i = 1; i < functions.size(); i++) {
            Function<?, ?> last = functions.get(i - 1);
            Function<?, ?> next = functions.get(i);
            Type returnType = functionReturnType(last);
            Type parameterType = functionParameterType(next);
            if (!GenericTypeReflector.isSuperType(parameterType, returnType)) {
                throw new RuntimeException("Pipeline functions do not compose: "
                        + "function at index " + (i - 1) + " has return type " + returnType
                        + " which is not assignable to " + parameterType
                        + ", the parameter type of function at index " + i);
            }
        }
    }

    private static Type[] functionTypeArguments(Function<?, ?> function) {
        Type functionType = GenericTypeReflector.getExactSuperType(function.getClass(), Function.class);
        if (functionType instanceof ParameterizedType pt
                && pt.getRawType().equals(Function.class)) {
            return pt.getActualTypeArguments();
        }
        else {
            throw new RuntimeException("Could not find Function supertype of " + functionType);
        }
    }

    private static Type functionParameterType(Function<?, ?> function) {
        return functionTypeArguments(function)[0];
    }

    private static Type functionReturnType(Function<?, ?> function) {
        return functionTypeArguments(function)[1];
    }
}

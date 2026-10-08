/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.transform.neo.ops.common;

import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;

import io.leangen.geantyref.TypeFactory;

import io.kroxylicious.transform.neo.op.BaseTypedOp;
import io.kroxylicious.transform.neo.op.OpContext;
import io.kroxylicious.transform.neo.op.OpFactory;
import io.kroxylicious.transform.service.TransformInitializationContext;

/**
 * Factory for the {@code SortedList} operation, which returns a sorted copy of the given list.
 * @param <T> The element type
 */
public class SortedList<T> implements OpFactory<List<T>, List<T>> {
    /**
     * Configuration for {@link SortedList}.
     */
    record Config() {

    }

    @Override
    public BaseTypedOp<List<T>, List<T>> create(Map<String, Object> configMap, TransformInitializationContext lookup, Type argumentType) {
        var config = Mapper.OP_CONFIG_MAPPER.convertValue(configMap, Config.class);

        Type listTypeArgument = OpTypeChecker.singleTypeArgumentOf(argumentType, List.class);
        Type resultType = TypeFactory.parameterizedClass(List.class, listTypeArgument);
        return BaseTypedOp.of(argumentType, resultType, (List<T> value, OpContext opContext) -> {
            return value.stream().sorted().toList();
        });
    }
}

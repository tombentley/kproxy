/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.record.manipulation.config;

import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;

import io.leangen.geantyref.TypeFactory;

import io.kroxylicious.filter.record.manipulation.common.PluginLookup;
import io.kroxylicious.filter.record.manipulation.op.BaseTypedOp;
import io.kroxylicious.filter.record.manipulation.op.OpContext;
import io.kroxylicious.filter.record.manipulation.op.OpFactory;
import io.kroxylicious.filter.record.manipulation.op.OpTypeChecker;

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
    public BaseTypedOp<List<T>, List<T>> create(Map<String, Object> configMap, PluginLookup lookup, Type argumentType) {
        var config = OpConfigs.OP_CONFIG_MAPPER.convertValue(configMap, Config.class);

        Type listTypeArgument = OpTypeChecker.singleTypeArgumentOf(argumentType, List.class);
        Type resultType = TypeFactory.parameterizedClass(List.class, listTypeArgument);
        return BaseTypedOp.of(argumentType, resultType, (List<T> value, OpContext opContext) -> {
            return value.stream().sorted().toList();
        });
    }
}

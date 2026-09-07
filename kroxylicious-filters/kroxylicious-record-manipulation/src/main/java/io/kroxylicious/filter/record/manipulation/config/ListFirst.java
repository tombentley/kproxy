/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.record.manipulation.config;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

import com.fasterxml.jackson.databind.JavaType;

import io.leangen.geantyref.GenericTypeReflector;

import io.kroxylicious.filter.record.manipulation.common.PluginLookup;
import io.kroxylicious.filter.record.manipulation.op.BaseTypedOp;
import io.kroxylicious.filter.record.manipulation.op.OpContext;
import io.kroxylicious.filter.record.manipulation.op.OpFactory;
import io.kroxylicious.filter.record.manipulation.op.TypeException;

/**
 * Factory for the {@code ListFirst} operation, which returns the first item from a list.
 * @param <T> The element type
 */
public class ListFirst<T> implements OpFactory<List<T>, T> {
    /**
     * Configuration for {@link ListFirst}.
     * @param ifEmpty The value to be returned if the list is empty. This will be converted
     * from the JSON type to the Java type {@code T}.
     * @param throwIfEmpty If true then throw if the list is empty, rather than returning a value
     */
    record Config(Object ifEmpty,
                  boolean throwIfEmpty) {

    }

    @Override
    public BaseTypedOp<List<T>, T> create(Map<String, Object> configMap, PluginLookup lookup, Type argumentType) {
        var config = OpConfigs.OP_CONFIG_MAPPER.convertValue(configMap, Config.class);
        // TODO we would be nice if we knew the TypeReference at this point
        // That would mean we'd need to type check the factories, so that I know what ListFirst is going to be called on
        Type listSuperType = GenericTypeReflector.getExactSuperType(argumentType, List.class);
        if (!(listSuperType instanceof ParameterizedType parameterizedType)) {
            throw new TypeException("Argument type " + GenericTypeReflector.getTypeName(argumentType) + " is not a subtype of " + List.class);
        }

        Type listTypeArgument = parameterizedType.getActualTypeArguments()[0];
        // if (listTypeArgument instanceof TypeVariable<?> || listTypeArgument instanceof WildcardType) {
        // throw new TypeException("List type argument " + GenericTypeReflector.getTypeName(listTypeArgument) + " is not supported");
        // }
        T ifEmpty;

        if (config.ifEmpty() != null) {
            ifEmpty = getT(listTypeArgument, config.ifEmpty());
        }
        else {
            ifEmpty = null;
        }
        var throwIfEmpty = config.throwIfEmpty();
        return BaseTypedOp.of(argumentType, listTypeArgument, (List<T> value, OpContext opContext) -> {
            if (value.isEmpty()) {
                if (throwIfEmpty) {
                    throw new NoSuchElementException("No first element of an empty list");
                }
                return ifEmpty;
            }
            return value.getFirst();
        });
    }

    private T getT(Type actualTypeArgument, Object ifEmptyObj) {
        JavaType jt;
        jt = OpConfigs.OP_CONFIG_MAPPER.getTypeFactory().constructType(actualTypeArgument);
        // if (actualTypeArgument instanceof Class c) {
        // jt = OpConfigs.MAPPER.getTypeFactory().constructType(c);
        // }
        // else if (actualTypeArgument instanceof ParameterizedType p
        // && p.getRawType() instanceof Class rawClass
        // && Arrays.stream(p.getActualTypeArguments()).allMatch(Class.class::isInstance)) {
        // jt = OpConfigs.MAPPER.getTypeFactory()
        // .constructParametricType(rawClass, Arrays.stream(p.getActualTypeArguments())
        // .map(typeArgument -> (Class<?>) typeArgument)
        // .toArray(Class[]::new));
        //
        // }
        // else {
        // throw new TypeException("Unable to map type " + GenericTypeReflector.getTypeName(actualTypeArgument));
        // }
        return (T) OpConfigs.OP_CONFIG_MAPPER.convertValue(ifEmptyObj, jt);
    }
}

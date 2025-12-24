/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.transformation.api.mapper;

import java.io.IOException;
import java.util.List;

import org.apache.kafka.common.header.Header;

import io.kroxylicious.filter.transformation.HeadersTransformation;

/**
 * <p>A typed, unary function for transformating values.</p>
 * @param <T> The parameter's Java type
 * @param <U> The returned value's Java type
 */
public interface Mapper<T, U> {

    static <T> Mapper<T, T> identity(Class<T> type) {
        return new Mapper<>() {
            @Override
            public Class<T> acceptedType() {
                return type;
            }

            @Override
            public Class<T> returnedType() {
                return type;
            }

            @Override
            public T transform(T value) {
                return value;
            }
        };
    }

    static Mapper<List<Header>, List<Header>> identityHeaders() {
        return Mapper.<List<Header>>identity((Class) List.class);
    }

    static <T> Mapper<List<T>, List<T>> emptyList() {
        return new Mapper<List<T>, List<T>>() {
            @Override
            public Class<List<T>> acceptedType() {
                return (Class) List.class;
            }

            @Override
            public Class<List<T>> returnedType() {
                return (Class) List.class;
            }

            @Override
            public List<T> transform(List<T> value) {
                return List.of();
            }
        };
    }


    /**
     * @return The class of the parameter's Java type
     */
    Class<T> acceptedType();

    /**
     * @return The class of the returned value's Java type
     */
    Class<U> returnedType();

    /**
     * Applies the transformation to the given value.
     * @param value The value to be transformed.
     * @return The transformed value.
     * @throws IOException
     */
    U transform(T value);
}

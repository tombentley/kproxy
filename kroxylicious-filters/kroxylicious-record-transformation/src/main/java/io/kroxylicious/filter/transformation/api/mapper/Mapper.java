/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.transformation.api.mapper;

import java.io.IOException;

/**
 * <p>A typed, unary function for transformating values.</p>
 * @param <T> The parameter's Java type
 * @param <U> The returned value's Java type
 */
public interface Mapper<T, U> {

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
     *
     * @param value The value to be transformed.
     * @param context
     * @return The transformed value.
     */
    U transform(T value, Context context);
}

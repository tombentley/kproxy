/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.transformation;

import java.io.IOException;

/**
 * <p>A typed, unary function on {@link Datum}s.</p>
 * <p>Mappers can, in general, define a mapping which returns a different schema id
 * and/or Java type than the argument datum.</p>
 * @param <T> The parameter datum's Java type
 * @param <U> The returned datum's Java type
 */
public interface DatumMapper<T, U> {

    /**
     * @return The class of the parameter datum's Java type
     */
    Class<T> acceptedType();

    /**
     * @return The class of the returned datum's Java type
     */
    Class<U> returnedType();

    /**
     * Applies the transformation to the given datum.
     * @param datum The datum to be transformed.
     * @return The transformed datum.
     * @throws IOException
     */
    U transform(T datum);
}

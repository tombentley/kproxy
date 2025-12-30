/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.transformation.api.mapper;

import io.kroxylicious.filter.transformation.api.SchemaAndValue;

public interface DataMapping<S, T, U, V>
        extends TypeCheckable {
    /**
     * Applies the transformation to the given value.
     *
     * @param value The value to be transformed.
     * @param context
     * @return The transformed value.
     */
    SchemaAndValue<U, V> transform(SchemaAndValue<S, T> value, Context context);
}

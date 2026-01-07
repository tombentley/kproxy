/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.transformation.aafresh;

import io.kroxylicious.filter.transformation.api.mapper.Context;

import edu.umd.cs.findbugs.annotations.Nullable;

public interface DataMapping2<V, V2> {
    /**
     * Applies the transformation to the given value.
     *
     * @param value The value to be transformed.
     * @param context The context
     * @return The transformed value.
     */
    @Nullable V2 transform(@Nullable V value, Context context);
}

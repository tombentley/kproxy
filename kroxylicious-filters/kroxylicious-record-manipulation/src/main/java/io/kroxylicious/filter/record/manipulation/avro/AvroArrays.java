/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.record.manipulation.avro;

import java.util.List;
import java.util.function.BiFunction;

import io.kroxylicious.filter.record.manipulation.common.Context;

/**
 * Mirrors {@link io.kroxylicious.filter.record.manipulation.jackson.ArrayNodes} for Avro array values.
 */
public class AvroArrays {

    private AvroArrays() {
    }

    /**
     * Maps the elements of an array.
     * @param itemsFn the function applied to each element of the array
     * @return a function mapping an array to a new array with {@code itemsFn} applied to each element
     */
    public static BiFunction<List<Object>, Context, List<Object>> items(BiFunction<Object, Context, Object> itemsFn) {
        return (array, context) -> new AvroArrayElements().modifyAll(array, itemsFn, context);
    }
}

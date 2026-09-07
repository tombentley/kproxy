/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.record.manipulation.avro;

import java.util.List;

import io.kroxylicious.filter.record.manipulation.op.BaseTypedOp;

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
    public static BaseTypedOp<List<Object>, List<Object>> items(BaseTypedOp<Object, Object> itemsFn) {
        return null; /*(array, context) -> new ListElements().modifyAll(array, new StaticTypedOp<Object, Object>() {
            @Override
            public Object apply(Object value, OpContext opContext) {
                return itemsFn.apply(value, opContext);
            }

            @Override
            public Type outputType(Type inputType) {
                return Object.class;
            }
        }, context);*/
    }
}

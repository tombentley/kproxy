/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.transform.neo.format.avro;

import java.lang.reflect.Type;
import java.util.List;

import io.kroxylicious.transform.neo.format.jackson2.ArrayNodes;
import io.kroxylicious.transform.neo.op.BaseTypedOp;
import io.kroxylicious.transform.neo.ops.common.ListElements;

/**
 * Mirrors {@link ArrayNodes} for Avro array values.
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
        // Raw List.class can't be expressed as Class<List<Object>>, so this must use the Type-based
        // overload rather than the Class-based one.
        return BaseTypedOp.of((Type) List.class, (Type) List.class,
                (array, context) -> new ListElements().modifyAll(array, itemsFn, context));
    }
}

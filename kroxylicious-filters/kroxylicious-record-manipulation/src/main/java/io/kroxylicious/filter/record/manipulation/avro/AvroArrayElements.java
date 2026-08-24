/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.record.manipulation.avro;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;

import io.kroxylicious.filter.record.manipulation.common.Context;
import io.kroxylicious.filter.record.manipulation.common.Traversal;

/**
 * A {@link Traversal} focusing on every element of an Avro array value.
 * <p>
 * Avro's generic API already represents an array as a plain {@link List} (a value read by
 * {@link org.apache.avro.generic.GenericDatumReader} comes back as a
 * {@link org.apache.avro.generic.GenericData.Array}, which implements {@link List}), and
 * {@link org.apache.avro.generic.GenericDatumWriter} accepts any {@link java.util.Collection} when
 * writing one back - so, unlike {@link io.kroxylicious.filter.record.manipulation.jackson.ArrayItems},
 * this needs no format-specific node type at either end.
 */
public record AvroArrayElements() implements Traversal<List<Object>, Object> {

    @Override
    public List<Object> getAll(List<Object> array) {
        return List.copyOf(array);
    }

    @Override
    public List<Object> modifyAll(List<Object> array, BiFunction<Object, Context, Object> f, Context context) {
        List<Object> result = new ArrayList<>(array.size());
        for (Object element : array) {
            result.add(f.apply(element, context));
        }
        return result;
    }
}

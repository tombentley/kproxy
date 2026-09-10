/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.record.manipulation.common;

import java.util.ArrayList;
import java.util.List;

import io.kroxylicious.filter.record.manipulation.format.jackson2.ArrayItems;
import io.kroxylicious.filter.record.manipulation.op.BaseTypedOp;
import io.kroxylicious.filter.record.manipulation.op.OpContext;

/**
 * A {@link Traversal} focusing on every element of a plain {@link List} - shared by every format whose
 * generic API already represents a repeated/array value as a {@code List} rather than a format-specific
 * node type (Avro's {@code array}, Protobuf's {@code repeated}), unlike
 * {@link ArrayItems}, which needs Jackson's
 * {@code ArrayNode} at both ends.
 */
public record ListElements() implements Traversal<List<Object>, Object> {

    @Override
    public List<Object> getAll(List<Object> list) {
        return List.copyOf(list);
    }

    @Override
    public List<Object> modifyAll(List<Object> list, BaseTypedOp<Object, Object> f, OpContext opContext) {
        List<Object> result = new ArrayList<>(list.size());
        for (Object element : list) {
            result.add(f.apply(element, opContext));
        }
        return result;
    }
}

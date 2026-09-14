/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.record.manipulation.xform.path;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.apache.avro.generic.GenericRecord;

public class AvroGenericTree implements TreeAdapter<Object> {
    @Override
    public boolean isObject(Object node) {
        return node instanceof GenericRecord || node instanceof Map<?, ?>;
    }

    @Override
    public boolean isArray(Object node) {
        return node instanceof Collection<?>;
    }

    @Override
    public Iterable<? extends Prop<Object>> objectProperties(Object node) {
        if (node instanceof GenericRecord record) {
            return record.getSchema().getFields().stream().map(field -> new Prop<>(field.name(), record.get(field.name()))).toList();
        }
        else if (node instanceof Map<?, ?> map) {
            return map.entrySet().stream().map(entry -> new Prop<>((String) entry.getKey(), (Object) entry.getValue())).toList();
        }
        throw new IllegalArgumentException("Unexpected node type: " + node.getClass());
    }

    @Override
    public int arrayLength(Object node) {
        return ((Collection<?>) node).size();
    }

    @Override
    public Object arrayItem(Object node, int index) {
        if (node instanceof Collection<?> collection) {
            if (collection instanceof List<?> list) {
                return ((List<?>) collection).get(index);
            }
            else {
                throw new IllegalArgumentException("Inefficient node type: " + node.getClass());
            }
        }
        throw new IllegalArgumentException("Unexpected node type: " + node.getClass());
    }
}

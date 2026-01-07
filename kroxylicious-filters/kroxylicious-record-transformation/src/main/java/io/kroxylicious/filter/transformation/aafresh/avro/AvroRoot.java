/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.transformation.aafresh.avro;

import java.util.Map;

import org.apache.avro.Schema;
import org.apache.avro.generic.GenericData;

public record AvroRoot(Object root, Schema schema) {

    private static final AvroRoot MISSING = new AvroRoot(null, null);

    AvroRoot path(String fieldName) {
        if (this == MISSING) {
            return MISSING;
        }
        if (root instanceof GenericData.Record record) {
            return new AvroRoot(record.get(fieldName), record.getSchema().getField(fieldName).schema());
        }
        else if (root instanceof Map<?, ?> map) {
            return new AvroRoot(map.get(fieldName), schema.getValueType());
        }
        return MISSING;
    }

    AvroRoot path(int index) {
        if (this == MISSING) {
            return MISSING;
        }
        if (root instanceof GenericData.AbstractArray array) {
            return new AvroRoot(array.get(index), array.getSchema().getElementType());
        }
    }
}

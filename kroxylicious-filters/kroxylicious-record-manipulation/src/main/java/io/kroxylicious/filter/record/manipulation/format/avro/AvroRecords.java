/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.record.manipulation.format.avro;

import java.util.Map;
import java.util.function.BiFunction;

import org.apache.avro.Schema;
import org.apache.avro.generic.GenericData;
import org.apache.avro.generic.GenericRecord;

import io.kroxylicious.filter.record.manipulation.format.jackson2.ObjectNodes;
import io.kroxylicious.filter.record.manipulation.op.BaseTypedOp;
import io.kroxylicious.filter.record.manipulation.op.OpContext;

/**
 * Mirrors {@link ObjectNodes} for Avro record values -
 * but simpler, because Avro requires every field declared by a {@link Schema} to be present in any
 * conforming {@link GenericRecord}. There is no per-field absence to model the way
 * {@link io.kroxylicious.filter.record.manipulation.common.Maybe} does for JSON object properties, so
 * {@link #mapFields} is a total map over {@code schema}'s declared fields rather than the insert/delete
 * capable machinery {@code ObjectNodes.mapProperties} needs - inserting or deleting a field isn't
 * meaningful yet without also supporting Avro's union/default mechanism, which is its own separate piece
 * of work.
 */
public class AvroRecords {

    private AvroRecords() {
    }

    /**
     * Maps selected fields of a record.
     * @param schema the schema {@code record} (and every result of the returned function) conforms to
     * @param fieldFns the per-field functions, keyed by field name; a field with no entry is carried
     *                 over unchanged
     * @return a function building a fresh {@link GenericRecord} per the rules above
     */
    public static BiFunction<GenericRecord, OpContext, GenericRecord> mapFields(
                                                                                Schema schema,
                                                                                Map<String, ? extends BaseTypedOp<Object, Object>> fieldFns) {
        return new AvroRecordFieldsFunction(schema, fieldFns);
    }

    private record AvroRecordFieldsFunction(Schema schema,
                                            Map<String, ? extends BaseTypedOp<Object, Object>> fieldFns)
            implements BiFunction<GenericRecord, OpContext, GenericRecord> {

        @Override
        public GenericRecord apply(GenericRecord record, OpContext opContext) {
            GenericData.Record result = new GenericData.Record(schema);
            for (Schema.Field field : schema.getFields()) {
                Object value = record.get(field.name());
                var fieldFn = fieldFns.get(field.name());
                result.put(field.name(), fieldFn != null ? fieldFn.apply(value, opContext) : value);
            }
            return result;
        }
    }
}

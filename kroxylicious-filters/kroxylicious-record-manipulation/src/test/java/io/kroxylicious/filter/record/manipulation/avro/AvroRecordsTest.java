/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.record.manipulation.avro;

import java.util.Random;

import org.apache.avro.Schema;
import org.apache.avro.generic.GenericData;
import org.apache.avro.generic.GenericRecord;

import io.kroxylicious.filter.record.manipulation.common.OpContext;

class AvroRecordsTest {

    private static final OpContext OP_CONTEXT = new OpContext(new Random(), new byte[0]);

    private static final Schema SCHEMA = new Schema.Parser().parse("""
            {"type": "record", "name": "Numbers", "fields": [
                {"name": "a", "type": "int"},
                {"name": "b", "type": "int"},
                {"name": "c", "type": "int"}
            ]}
            """);

    private static GenericRecord record(int a, int b, int c) {
        GenericRecord record = new GenericData.Record(SCHEMA);
        record.put("a", a);
        record.put("b", b);
        record.put("c", c);
        return record;
    }

//    @Test
//    void mapFieldsReplacesOnlyTheFieldsPresentInTheMap() {
//        // Given
//        GenericRecord input = record(1, 2, 3);
//        BaseTypedOp<Object, Object> incrementFn = (value, context) -> (Integer) value + 1;
//
//        // When
//        GenericRecord result = AvroRecords.mapFields(SCHEMA, Map.of("a", incrementFn)).apply(input, OP_CONTEXT);
//
//        // Then
//        assertThat(result.get("a")).isEqualTo(2);
//        assertThat(result.get("b")).isEqualTo(2);
//        assertThat(result.get("c")).isEqualTo(3);
//    }
//
//    @Test
//    void mapFieldsDoesNotMutateTheInputRecord() {
//        // Given
//        GenericRecord input = record(1, 2, 3);
//        BaseTypedOp<Integer, Integer> incrementFn = new StaticTypedOp<Integer, Integer>() {
//            @Override
//            public Type outputType(Type inputType) {
//                return null;
//            }
//
//            @Override
//            public Integer apply(Integer value, OpContext opContext) {
//                return value + 1;
//            }
//        };
//
//        // When
//        var unused = AvroRecords.mapFields(SCHEMA, Map.of("a", incrementFn)).apply(input, OP_CONTEXT);
//
//        // Then
//        assertThat(input.get("a")).isEqualTo(1);
//    }

}

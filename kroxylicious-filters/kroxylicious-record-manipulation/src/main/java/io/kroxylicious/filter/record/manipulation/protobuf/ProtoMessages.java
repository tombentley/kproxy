/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.record.manipulation.protobuf;

import java.util.Map;
import java.util.function.BiFunction;

import com.google.protobuf.Descriptors;
import com.google.protobuf.DynamicMessage;

import io.kroxylicious.filter.record.manipulation.common.Context;

/**
 * Mirrors {@link io.kroxylicious.filter.record.manipulation.avro.AvroRecords} for Protobuf message values,
 * with one difference: unlike Avro, a Protobuf field can genuinely be absent (a field only lacks presence
 * tracking - {@link Descriptors.FieldDescriptor#hasPresence()} - for proto3 implicit-presence scalars and
 * repeated fields, which always read back a value, default or not). A field this module has no function
 * for is carried over exactly as-is, present or absent; masking never manufactures presence a record didn't
 * already have.
 */
public class ProtoMessages {

    private ProtoMessages() {
    }

    /**
     * Maps selected fields of a message.
     * @param descriptor the schema {@code message} (and every result of the returned function) conforms to
     * @param fieldFns the per-field functions, keyed by field name; a field with no entry is carried
     *                 over unchanged
     * @return a function building a fresh {@link DynamicMessage} per the rules above
     */
    public static BiFunction<DynamicMessage, Context, DynamicMessage> mapFields(
                                                                                Descriptors.Descriptor descriptor,
                                                                                Map<String, ? extends BiFunction<Object, Context, Object>> fieldFns) {
        return new ProtoMessageFieldsFunction(descriptor, fieldFns);
    }

    private record ProtoMessageFieldsFunction(Descriptors.Descriptor descriptor,
                                              Map<String, ? extends BiFunction<Object, Context, Object>> fieldFns)
            implements BiFunction<DynamicMessage, Context, DynamicMessage> {

        @Override
        public DynamicMessage apply(DynamicMessage message, Context context) {
            DynamicMessage.Builder result = DynamicMessage.newBuilder(descriptor);
            for (Descriptors.FieldDescriptor field : descriptor.getFields()) {
                if (field.hasPresence() && !message.hasField(field)) {
                    continue;
                }
                Object value = message.getField(field);
                var fieldFn = fieldFns.get(field.getName());
                result.setField(field, fieldFn != null ? fieldFn.apply(value, context) : value);
            }
            return result.build();
        }
    }
}

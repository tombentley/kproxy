/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.record.manipulation.format.protobuf;

import java.util.Map;
import java.util.Random;

import org.junit.jupiter.api.Test;

import com.google.protobuf.Descriptors;
import com.google.protobuf.DynamicMessage;

import io.kroxylicious.filter.record.manipulation.op.OpContext;

import static org.assertj.core.api.Assertions.assertThat;

class ProtobufMessagesTest {

    private static final OpContext OP_CONTEXT = new OpContext(new Random(), new byte[0]);

    private static final Descriptors.Descriptor DESCRIPTOR = ProtobufSchemaParser.parse("""
            syntax = "proto3";
            message Numbers {
                int32 a = 1;
                int32 b = 2;
                int32 c = 3;
                Nested nested = 4;
                message Nested {
                    string value = 1;
                }
            }
            """, "Numbers").descriptor();

    private static DynamicMessage numbers(int a, int b, int c) {
        return DynamicMessage.newBuilder(DESCRIPTOR)
                .setField(DESCRIPTOR.findFieldByName("a"), a)
                .setField(DESCRIPTOR.findFieldByName("b"), b)
                .setField(DESCRIPTOR.findFieldByName("c"), c)
                .build();
    }

//    @Test
//    void mapFieldsReplacesOnlyTheFieldsPresentInTheMap() {
//        // Given
//        DynamicMessage input = numbers(1, 2, 3);
//        BaseTypedOp<Object, Object> incrementFn = (value, context) -> (Integer) value + 1;
//
//        // When
//        DynamicMessage result = ProtobufMessages.mapFields(DESCRIPTOR, Map.of("a", incrementFn)).apply(input, OP_CONTEXT);
//
//        // Then
//        assertThat(result.getField(DESCRIPTOR.findFieldByName("a"))).isEqualTo(2);
//        assertThat(result.getField(DESCRIPTOR.findFieldByName("b"))).isEqualTo(2);
//        assertThat(result.getField(DESCRIPTOR.findFieldByName("c"))).isEqualTo(3);
//    }

//    @Test
//    void mapFieldsDoesNotMutateTheInputMessage() {
//        // Given
//        DynamicMessage input = numbers(1, 2, 3);
//        BaseTypedOp<Object, Object> incrementFn = (value, context) -> (Integer) value + 1;
//
//        // When
//        var unused = ProtobufMessages.mapFields(DESCRIPTOR, Map.of("a", incrementFn)).apply(input, OP_CONTEXT);
//
//        // Then
//        assertThat(input.getField(DESCRIPTOR.findFieldByName("a"))).isEqualTo(1);
//    }

    @Test
    void mapFieldsLeavesAnAbsentPresenceTrackedFieldAbsent() {
        // Given
        DynamicMessage input = numbers(1, 2, 3);

        // When
        DynamicMessage result = ProtobufMessages.mapFields(DESCRIPTOR, Map.of()).apply(input, OP_CONTEXT);

        // Then
        assertThat(result.hasField(DESCRIPTOR.findFieldByName("nested"))).isFalse();
    }

}

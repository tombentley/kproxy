/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.record.manipulation.format.protobuf;

import java.util.List;
import java.util.Random;

import org.junit.jupiter.api.Test;

import com.google.protobuf.ByteString;
import com.google.protobuf.Descriptors;
import com.google.protobuf.DynamicMessage;

import io.kroxylicious.filter.record.manipulation.common.PluginLookup;
import io.kroxylicious.filter.record.manipulation.common.ServiceLoaderPluginLookup;
import io.kroxylicious.filter.record.manipulation.op.OpContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProtobufFunctionTest {

    private static final OpContext OP_CONTEXT = new OpContext(new Random(), new byte[0]);
    private static final PluginLookup LOOKUP = new ServiceLoaderPluginLookup();

    private static ParsedProtoSchema parse(String protoText, String rootMessageName) {
        return ProtobufSchemaParser.parse(protoText, rootMessageName);
    }

    @Test
    void buildMaskAppliesStringOpToAnnotatedField() {
        // Given
        ParsedProtoSchema schema = parse("""
                syntax = "proto3";
                message R {
                    string first_name = 1 [(apply) = { op: "ValueString", value: "REDACTED" }];
                    string surname = 2;
                }
                """, "R");
        Descriptors.Descriptor descriptor = schema.descriptor();
        DynamicMessage input = DynamicMessage.newBuilder(descriptor)
                .setField(descriptor.findFieldByName("first_name"), "Harry")
                .setField(descriptor.findFieldByName("surname"), "Potter")
                .build();

        // When
        DynamicMessage result = (DynamicMessage) ProtobufFunction.buildMask(schema, LOOKUP).apply(input, OP_CONTEXT);

        // Then
        assertThat(result.getField(descriptor.findFieldByName("first_name"))).isEqualTo("REDACTED");
        assertThat(result.getField(descriptor.findFieldByName("surname"))).isEqualTo("Potter");
    }

    @Test
    void buildMaskAppliesIntOpToAnnotatedField() {
        // Given
        ParsedProtoSchema schema = parse("""
                syntax = "proto3";
                message R {
                    int32 age_years = 1 [(apply) = { op: "ValueInt", value: 99 }];
                }
                """, "R");
        Descriptors.Descriptor descriptor = schema.descriptor();
        DynamicMessage input = DynamicMessage.newBuilder(descriptor)
                .setField(descriptor.findFieldByName("age_years"), 17)
                .build();

        // When
        DynamicMessage result = (DynamicMessage) ProtobufFunction.buildMask(schema, LOOKUP).apply(input, OP_CONTEXT);

        // Then
        assertThat(result.getField(descriptor.findFieldByName("age_years"))).isEqualTo(99);
    }

    @Test
    void buildMaskRecursesIntoNestedMessageFields() {
        // Given
        ParsedProtoSchema schema = parse("""
                syntax = "proto3";
                message R {
                    message Address {
                        string city = 1 [(apply) = { op: "ValueString", value: "REDACTED" }];
                    }
                    Address address = 1;
                }
                """, "R");
        Descriptors.Descriptor descriptor = schema.descriptor();
        Descriptors.Descriptor addressDescriptor = descriptor.findNestedTypeByName("Address");
        DynamicMessage address = DynamicMessage.newBuilder(addressDescriptor)
                .setField(addressDescriptor.findFieldByName("city"), "Hogsmeade")
                .build();
        DynamicMessage input = DynamicMessage.newBuilder(descriptor)
                .setField(descriptor.findFieldByName("address"), address)
                .build();

        // When
        DynamicMessage result = (DynamicMessage) ProtobufFunction.buildMask(schema, LOOKUP).apply(input, OP_CONTEXT);

        // Then
        DynamicMessage resultAddress = (DynamicMessage) result.getField(descriptor.findFieldByName("address"));
        assertThat(resultAddress.getField(addressDescriptor.findFieldByName("city"))).isEqualTo("REDACTED");
    }

    @Test
    void buildMaskAppliesOpToEachRepeatedElement() {
        // Given
        ParsedProtoSchema schema = parse("""
                syntax = "proto3";
                message R {
                    repeated string aliases = 1 [(apply) = { op: "ValueString", value: "REDACTED" }];
                }
                """, "R");
        Descriptors.Descriptor descriptor = schema.descriptor();
        Descriptors.FieldDescriptor aliases = descriptor.findFieldByName("aliases");
        DynamicMessage input = DynamicMessage.newBuilder(descriptor)
                .setField(aliases, List.of("Vernon Dudley", "Barny Weasley"))
                .build();

        // When
        DynamicMessage result = (DynamicMessage) ProtobufFunction.buildMask(schema, LOOKUP).apply(input, OP_CONTEXT);

        // Then
        @SuppressWarnings("unchecked")
        List<Object> resultAliases = (List<Object>) result.getField(aliases);
        assertThat(resultAliases).containsExactly("REDACTED", "REDACTED");
    }

    @Test
    void buildMaskAppliesBytesOpToAnnotatedField() {
        // Given
        ParsedProtoSchema schema = parse("""
                syntax = "proto3";
                message R {
                    bytes photo = 1 [(apply) = { op: "ValueBytes", value: [9, 9, 9] }];
                }
                """, "R");
        Descriptors.Descriptor descriptor = schema.descriptor();
        DynamicMessage input = DynamicMessage.newBuilder(descriptor)
                .setField(descriptor.findFieldByName("photo"), ByteString.copyFrom(new byte[]{ 1, 2, 3 }))
                .build();

        // When
        DynamicMessage result = (DynamicMessage) ProtobufFunction.buildMask(schema, LOOKUP).apply(input, OP_CONTEXT);

        // Then
        assertThat((ByteString) result.getField(descriptor.findFieldByName("photo"))).isEqualTo(ByteString.copyFrom(new byte[]{ 9, 9, 9 }));
    }

    @Test
    void buildMaskAppliesEnumOpAndAcceptsAllowedSymbol() {
        // Given
        ParsedProtoSchema schema = parse("""
                syntax = "proto3";
                message R {
                    enum Color { RED = 0; GREEN = 1; BLUE = 2; }
                    Color color = 1 [(apply) = { op: "ValueString", value: "GREEN" }];
                }
                """, "R");
        Descriptors.Descriptor descriptor = schema.descriptor();
        Descriptors.FieldDescriptor color = descriptor.findFieldByName("color");
        DynamicMessage input = DynamicMessage.newBuilder(descriptor)
                .setField(color, color.getEnumType().findValueByName("RED"))
                .build();

        // When
        DynamicMessage result = (DynamicMessage) ProtobufFunction.buildMask(schema, LOOKUP).apply(input, OP_CONTEXT);

        // Then
        assertThat(result.getField(color)).isEqualTo(color.getEnumType().findValueByName("GREEN"));
    }

    @Test
    void buildMaskThrowsWhenEnumOpProducesDisallowedSymbol() {
        // Given
        ParsedProtoSchema schema = parse("""
                syntax = "proto3";
                message R {
                    enum Color { RED = 0; GREEN = 1; BLUE = 2; }
                    Color color = 1 [(apply) = { op: "ValueString", value: "PURPLE" }];
                }
                """, "R");
        Descriptors.Descriptor descriptor = schema.descriptor();
        Descriptors.FieldDescriptor color = descriptor.findFieldByName("color");
        DynamicMessage input = DynamicMessage.newBuilder(descriptor)
                .setField(color, color.getEnumType().findValueByName("RED"))
                .build();
        var mask = ProtobufFunction.buildMask(schema, LOOKUP);

        // When/Then
        assertThatThrownBy(() -> mask.apply(input, OP_CONTEXT))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("PURPLE");
    }

    @Test
    void buildMaskPassesThroughIntWireVariantsAndEnumFieldsWithNoApplyConfigured() {
        // Given
        ParsedProtoSchema schema = parse("""
                syntax = "proto3";
                message R {
                    sint32 a = 1;
                    fixed32 b = 2;
                    enum Color { RED = 0; GREEN = 1; }
                    Color color = 3;
                }
                """, "R");
        Descriptors.Descriptor descriptor = schema.descriptor();
        Descriptors.FieldDescriptor color = descriptor.findFieldByName("color");
        DynamicMessage input = DynamicMessage.newBuilder(descriptor)
                .setField(descriptor.findFieldByName("a"), 5)
                .setField(descriptor.findFieldByName("b"), 6)
                .setField(color, color.getEnumType().findValueByName("RED"))
                .build();

        // When
        DynamicMessage result = (DynamicMessage) ProtobufFunction.buildMask(schema, LOOKUP).apply(input, OP_CONTEXT);

        // Then
        assertThat(result.getField(descriptor.findFieldByName("a"))).isEqualTo(5);
        assertThat(result.getField(descriptor.findFieldByName("b"))).isEqualTo(6);
        assertThat(result.getField(color)).isEqualTo(color.getEnumType().findValueByName("RED"));
    }

}

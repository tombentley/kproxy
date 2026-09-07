/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.record.manipulation.protobuf;

import java.util.Map;

import org.junit.jupiter.api.Test;

import com.google.protobuf.Descriptors;

import io.kroxylicious.filter.record.manipulation.op.OpConfig;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProtobufSchemaParserTest {

    @Test
    void parsesAMinimalMessageWithNoApply() {
        // Given
        String proto = """
                syntax = "proto3";
                message User {
                    string first_name = 1;
                }
                """;

        // When
        ParsedProtoSchema schema = ProtobufSchemaParser.parse(proto, "User");

        // Then
        assertThat(schema.descriptor().getName()).isEqualTo("User");
        assertThat(schema.descriptor().findFieldByName("first_name").getType()).isEqualTo(Descriptors.FieldDescriptor.Type.STRING);
        assertThat(schema.apply()).isEmpty();
    }

    @Test
    void readsAnApplyOptionOffAField() {
        // Given
        String proto = """
                syntax = "proto3";
                message User {
                    string first_name = 1 [(apply) = { op: "ValueString", value: "REDACTED" }];
                }
                """;

        // When
        ParsedProtoSchema schema = ProtobufSchemaParser.parse(proto, "User");

        // Then
        Descriptors.FieldDescriptor firstName = schema.descriptor().findFieldByName("first_name");
        assertThat(schema.apply().get(firstName)).containsExactly(new OpConfig("ValueString", Map.of("value", "REDACTED")));
    }

    @Test
    void collectsComposedApplyChainInDeclaredOrder() {
        // Given
        String proto = """
                syntax = "proto3";
                message User {
                    string city = 1 [(apply) = { op: "HmacString", keyId: "FOO" }, (apply) = { op: "EncryptString", keyId: "FOO" }];
                }
                """;

        // When
        ParsedProtoSchema schema = ProtobufSchemaParser.parse(proto, "User");

        // Then
        Descriptors.FieldDescriptor city = schema.descriptor().findFieldByName("city");
        assertThat(schema.apply().get(city)).hasSize(2);
        assertThat(schema.apply().get(city).get(0).op()).isEqualTo("HmacString");
        assertThat(schema.apply().get(city).get(1).op()).isEqualTo("EncryptString");
    }

    @Test
    void readsAnApplyOptionOffANestedMessage() {
        // Given
        String proto = """
                syntax = "proto3";
                message User {
                    message Address {
                        string city = 1 [(apply) = { op: "ValueString", value: "REDACTED" }];
                    }
                    Address address = 1;
                }
                """;

        // When
        ParsedProtoSchema schema = ProtobufSchemaParser.parse(proto, "User");

        // Then
        Descriptors.Descriptor address = schema.descriptor().findNestedTypeByName("Address");
        assertThat(schema.apply().get(address.findFieldByName("city"))).isNotNull();
    }

    @Test
    void throwsForAMessageNameNotDefinedInTheSchema() {
        // Given
        String proto = """
                syntax = "proto3";
                message User {
                    string first_name = 1;
                }
                """;

        // When/Then
        assertThatThrownBy(() -> ProtobufSchemaParser.parse(proto, "DoesNotExist")).isInstanceOf(RuntimeException.class);
    }
}

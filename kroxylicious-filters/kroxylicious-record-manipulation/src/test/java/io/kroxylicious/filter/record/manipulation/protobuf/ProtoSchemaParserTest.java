/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.record.manipulation.protobuf;

import org.junit.jupiter.api.Test;

import com.google.protobuf.Descriptors;

import io.kroxylicious.filter.record.manipulation.config.ApplyConfig;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProtoSchemaParserTest {

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
        ParsedProtoSchema schema = ProtoSchemaParser.parse(proto, "User");

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
                    string first_name = 1 [(apply) = { value: "REDACTED" }];
                }
                """;

        // When
        ParsedProtoSchema schema = ProtoSchemaParser.parse(proto, "User");

        // Then
        Descriptors.FieldDescriptor firstName = schema.descriptor().findFieldByName("first_name");
        assertThat(schema.apply().get(firstName)).containsExactly(new ApplyConfig(textNode("REDACTED"), null, null, null, null, null, null));
    }

    @Test
    void collectsComposedApplyChainInDeclaredOrder() {
        // Given
        String proto = """
                syntax = "proto3";
                message User {
                    string city = 1 [(apply) = { hmac: { keyId: "FOO" } }, (apply) = { encrypt: { keyId: "FOO" } }];
                }
                """;

        // When
        ParsedProtoSchema schema = ProtoSchemaParser.parse(proto, "User");

        // Then
        Descriptors.FieldDescriptor city = schema.descriptor().findFieldByName("city");
        assertThat(schema.apply().get(city)).hasSize(2);
        assertThat(schema.apply().get(city).get(0).hmac()).isNotNull();
        assertThat(schema.apply().get(city).get(1).encrypt()).isNotNull();
    }

    @Test
    void readsAnApplyOptionOffANestedMessage() {
        // Given
        String proto = """
                syntax = "proto3";
                message User {
                    message Address {
                        string city = 1 [(apply) = { value: "REDACTED" }];
                    }
                    Address address = 1;
                }
                """;

        // When
        ParsedProtoSchema schema = ProtoSchemaParser.parse(proto, "User");

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
        assertThatThrownBy(() -> ProtoSchemaParser.parse(proto, "DoesNotExist")).isInstanceOf(RuntimeException.class);
    }

    private static com.fasterxml.jackson.databind.JsonNode textNode(String value) {
        return com.fasterxml.jackson.databind.node.TextNode.valueOf(value);
    }
}

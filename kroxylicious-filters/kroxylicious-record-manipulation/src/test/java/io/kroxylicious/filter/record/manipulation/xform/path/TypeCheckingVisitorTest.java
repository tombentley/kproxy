/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.record.manipulation.xform.path;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import io.kroxylicious.filter.record.manipulation.format.jackson3.JsonSchemaTypeSystem;
import io.kroxylicious.filter.record.manipulation.op.BaseTypedOp;
import io.kroxylicious.filter.record.manipulation.type.SchemaType;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.JsonNodeFactory;
import tools.jackson.databind.node.ObjectNode;
import tools.jackson.databind.node.StringNode;
import tools.jackson.dataformat.yaml.YAMLMapper;

import static org.assertj.core.api.Assertions.assertThat;

class TypeCheckingVisitorTest {

    private static final JsonNode OBJECT_SCHEMA = new YAMLMapper().readTree("""
            type: object
            properties:
              foo:
                type: string
            """);

    private static SchemaType<JsonNode> jsonSchema() {
        return SchemaType.of(new JsonSchemaTypeSystem(new JsonNodeFactory()), OBJECT_SCHEMA);
    }

    /** A {@code read($.value)} call whose resolved op is {@code op}. */
    private static Ast.FnCall readValue(BaseTypedOp<?, ?> op) {
        Ast arg = new Ast.Path<>(Identifier.ROOT, new Ast.Segment.Child<>(new Ast.Selector.Name<>("value")), null);
        Ast.FnCall call = new Ast.FnCall("read", List.of(arg));
        call.setDecl(op);
        return call;
    }

    @Test
    void switchesActiveTypeAndRefinesOutputToSchemaType() {
        // Given
        SchemaType<JsonNode> introduced = jsonSchema();
        BaseTypedOp<ByteBuffer, JsonNode> op = BaseTypedOp.of(ByteBuffer.class, JsonNode.class, (value, opContext) -> null, introduced);
        Ast.FnCall call = readValue(op);
        var errors = new ArrayList<String>();
        var visitor = new TypeCheckingVisitor("value", ByteBuffer.class, errors);

        // When
        call.accept(visitor);

        // Then
        assertThat(errors).isEmpty();
        assertThat(call.type()).isEqualTo(ObjectNode.class); // JsonNode narrowed to the schema's logical type
        assertThat(visitor.activeType().property("foo").javaType()).isEqualTo(StringNode.class);
    }

    @Test
    void switchesActiveTypeButKeepsNonAssignableDeclaredOutput() {
        // Given
        SchemaType<JsonNode> introduced = jsonSchema();
        // Long is not a supertype of the schema's logical ObjectNode, so the declared output must be preserved
        BaseTypedOp<ByteBuffer, Long> op = BaseTypedOp.of(ByteBuffer.class, Long.class, (value, opContext) -> null, introduced);
        Ast.FnCall call = readValue(op);
        var visitor = new TypeCheckingVisitor("value", ByteBuffer.class, new ArrayList<>());

        // When
        call.accept(visitor);

        // Then
        assertThat(call.type()).isEqualTo(Long.class);
        assertThat(visitor.activeType().isObject()).isTrue();
    }
}

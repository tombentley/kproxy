/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.transform.neo.xform.path;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Random;

import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.junit.jupiter.api.Test;

import io.kroxylicious.kafka.common.header.Header;
import io.kroxylicious.kafka.common.header.internals.RecordHeader;
import io.kroxylicious.kafka.common.record.internal.Record;
import io.kroxylicious.transform.neo.ExprLexer;
import io.kroxylicious.transform.neo.ExprParser;
import io.kroxylicious.transform.neo.op.OpContext;
import io.kroxylicious.transform.neo.type.Nothing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Evaluates a whole top-level field expression against a Kafka {@link Record}, producing its value — the
 * "evaluate the transformation expressed in an AST" end-to-end path, for value (non-function) expressions.
 */
class TopLevelEvaluationTest {

    private static final OpContext OP_CONTEXT = new OpContext(new Random(0), new byte[0]);

    private static Object evaluate(Object root, String expression) {
        var lexer = new ExprLexer(CharStreams.fromString(expression));
        var parser = new ExprParser(new CommonTokenStream(lexer));
        Ast ast = parser.expr().accept(new AstBuilder());
        return new AstEvaluator<>(new KafkaRecordTree()).compileValue(ast).evaluate(root, OP_CONTEXT);
    }

    @Test
    void evaluatesScalarField() {
        // Given
        var record = mock(Record.class);
        when(record.timestamp()).thenReturn(42L);

        // When / Then
        assertThat(evaluate(record, "$.timestamp")).isEqualTo(42L);
    }

    @Test
    void evaluatesByteBufferField() {
        // Given
        var record = mock(Record.class);
        var value = ByteBuffer.allocate(8);
        when(record.value()).thenReturn(value);

        // When / Then
        assertThat(evaluate(record, "$.value")).isSameAs(value);
    }

    @Test
    void evaluatesNestedPathIntoHeaders() {
        // Given
        var record = mock(Record.class);
        Header[] headers = { new RecordHeader("trace-id", "abc".getBytes(StandardCharsets.UTF_8)) };
        when(record.headers()).thenReturn(headers);

        // When / Then
        assertThat(evaluate(record, "$.headers[0].key")).isEqualTo("trace-id");
    }

    @Test
    void absentFieldEvaluatesToNothing() {
        // Given
        var record = mock(Record.class);

        // When / Then: a path that selects no node yields the canonical absence value
        assertThat(evaluate(record, "$.headers[3].key")).isSameAs(Nothing.INSTANCE);
    }
}

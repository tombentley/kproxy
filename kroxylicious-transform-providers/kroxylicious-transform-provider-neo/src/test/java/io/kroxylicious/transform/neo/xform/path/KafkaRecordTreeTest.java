/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.transform.neo.xform.path;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Random;

import org.junit.jupiter.api.Test;

import io.kroxylicious.kafka.common.header.Header;
import io.kroxylicious.kafka.common.header.internals.RecordHeader;
import io.kroxylicious.transform.neo.op.OpContext;
import io.kroxylicious.transform.records.RecordView;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class KafkaRecordTreeTest {

    private static final OpContext OP_CONTEXT = new OpContext(new Random(0), new byte[0]);

    private static List<Object> select(Object root, Ast.Path<Object> path) {
        return new AstEvaluator<>(new KafkaRecordTree()).compileQuery(path).evaluate(root, OP_CONTEXT);
    }

    @Test
    void canAccessRecordTimestamp() {
        // Given
        var mockRecord = mock(RecordView.class);
        when(mockRecord.timestamp()).thenReturn(42L);

        // When
        var result = select(mockRecord, new Ast.Path<>(Identifier.ROOT, new Ast.Segment.Child<>(new Ast.Selector.Name<>("timestamp")), null));

        // Then
        assertThat(result).singleElement().isEqualTo(42L);
    }

    @Test
    void canAccessRecordKey() {
        // Given
        var mockRecord = mock(RecordView.class);
        var valueBuffer = ByteBuffer.allocate(100);
        when(mockRecord.key()).thenReturn(valueBuffer);

        // When
        var result = select(mockRecord, new Ast.Path<>(Identifier.ROOT, new Ast.Segment.Child<>(new Ast.Selector.Name<>("key")), null));

        // Then
        assertThat(result).singleElement().isSameAs(valueBuffer);
    }

    @Test
    void canAccessRecordValue() {
        // Given
        var mockRecord = mock(RecordView.class);
        var valueBuffer = ByteBuffer.allocate(100);
        when(mockRecord.value()).thenReturn(valueBuffer);

        // When
        var result = select(mockRecord, new Ast.Path<>(Identifier.ROOT, new Ast.Segment.Child<>(new Ast.Selector.Name<>("value")), null));

        // Then
        assertThat(result).singleElement().isSameAs(valueBuffer);
    }

    @Test
    void canAccessHeader() {
        // Given
        var mockRecord = mock(RecordView.class);
        Header[] headers = { new RecordHeader("my-key", "my-value".getBytes(StandardCharsets.UTF_8)) };
        when(mockRecord.headers()).thenReturn(headers);

        // When
        var result = select(mockRecord, new Ast.Path<>(Identifier.ROOT, new Ast.Segment.Child<>(new Ast.Selector.Name<>("headers")), null));

        // Then
        assertThat(result).singleElement().isSameAs(headers);
    }

    @Test
    void canAccessHeaderKey() {
        // Given
        var mockRecord = mock(RecordView.class);
        Header[] headers = { new RecordHeader("my-key", "my-value".getBytes(StandardCharsets.UTF_8)) };
        when(mockRecord.headers()).thenReturn(headers);

        // When
        var result = select(mockRecord, new Ast.Path<>(Identifier.ROOT,
                List.of(new Ast.Segment.Child<>(new Ast.Selector.Name<>("headers")),
                        new Ast.Segment.Child<>(new Ast.Selector.Children<>()),
                        new Ast.Segment.Child<>(new Ast.Selector.Name<>("key"))),
                null));

        // Then
        assertThat(result).singleElement().isEqualTo("my-key");
    }

    @Test
    void canAccessHeaderValue() {
        // Given
        var mockRecord = mock(RecordView.class);
        byte[] bytes = "my-value".getBytes(StandardCharsets.UTF_8);
        Header[] headers = { new RecordHeader("my-key", bytes) };
        when(mockRecord.headers()).thenReturn(headers);

        // When
        var result = select(mockRecord, new Ast.Path<>(Identifier.ROOT,
                List.of(new Ast.Segment.Child<>(new Ast.Selector.Name<>("headers")),
                        new Ast.Segment.Child<>(new Ast.Selector.Children<>()),
                        new Ast.Segment.Child<>(new Ast.Selector.Name<>("value"))),
                null));

        // Then
        assertThat(result).singleElement().isSameAs(bytes);
    }

}

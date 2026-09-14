/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.record.manipulation.xform.path;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import io.kroxylicious.kafka.common.header.Header;
import io.kroxylicious.kafka.common.header.internals.RecordHeader;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class KafkaRecordTreeTest {

    @Test
    void canAccessRecordTimestamp() {
        var mockRecord = mock(io.kroxylicious.kafka.common.record.internal.Record.class);
        when(mockRecord.timestamp()).thenReturn(42L);

        List<Object> result = new ArrayList<>();
        new PathEvaluator<>(new KafkaRecordTree()).eval(mockRecord, new Path<>(Identifier.ROOT, new Segment.Child<>(new Selector.Name<>("timestamp")), result::add));
        assertThat(result).singleElement().isEqualTo(42L);

        //Mockito.verifyNoMoreInteractions(mockRecord);
    }

    @Test
    void canAccessRecordKey() {
        var mockRecord = mock(io.kroxylicious.kafka.common.record.internal.Record.class);
        var valueBuffer = ByteBuffer.allocate(100);
        when(mockRecord.key()).thenReturn(valueBuffer);

        List<Object> result = new ArrayList<>();
        new PathEvaluator<>(new KafkaRecordTree()).eval(mockRecord, new Path<>(Identifier.ROOT, new Segment.Child<>(new Selector.Name<>("key")), result::add));
        assertThat(result).singleElement().isSameAs(valueBuffer);

        //Mockito.verifyNoMoreInteractions(mockRecord);
    }

    @Test
    void canAccessRecordValue() {
        var mockRecord = mock(io.kroxylicious.kafka.common.record.internal.Record.class);
        var valueBuffer = ByteBuffer.allocate(100);
        when(mockRecord.value()).thenReturn(valueBuffer);

        List<Object> result = new ArrayList<>();
        new PathEvaluator<>(new KafkaRecordTree()).eval(mockRecord, new Path<>(Identifier.ROOT, new Segment.Child<>(new Selector.Name<>("value")), result::add));
        assertThat(result).singleElement().isSameAs(valueBuffer);

        //Mockito.verifyNoMoreInteractions(mockRecord);
    }

    @Test
    void canAccessHeader() {
        var mockRecord = mock(io.kroxylicious.kafka.common.record.internal.Record.class);

        Header[] headers = { new RecordHeader("my-key", "my-value".getBytes(StandardCharsets.UTF_8)) };
        when(mockRecord.headers()).thenReturn(headers);

        List<Object> result = new ArrayList<>();
        new PathEvaluator<>(new KafkaRecordTree()).eval(mockRecord, new Path<>(Identifier.ROOT, new Segment.Child<>(new Selector.Name<>("headers")), result::add));
        assertThat(result).singleElement().isSameAs(headers);

        //Mockito.verifyNoMoreInteractions(mockRecord);
    }

    @Test
    void canAccessHeaderKey() {
        var mockRecord = mock(io.kroxylicious.kafka.common.record.internal.Record.class);

        Header[] headers = { new RecordHeader("my-key", "my-value".getBytes(StandardCharsets.UTF_8)) };
        when(mockRecord.headers()).thenReturn(headers);

        List<Object> result = new ArrayList<>();
        new PathEvaluator<>(new KafkaRecordTree()).eval(mockRecord, new Path<>(Identifier.ROOT,
                List.of(new Segment.Child<>(new Selector.Name<>("headers")),
                        new Segment.Child<>(new Selector.Children<>()),
                        new Segment.Child<>(new Selector.Name<>("key"))), result::add));
        assertThat(result).singleElement().isEqualTo("my-key");

        //Mockito.verifyNoMoreInteractions(mockRecord);
    }

    @Test
    void canAccessHeaderValue() {
        var mockRecord = mock(io.kroxylicious.kafka.common.record.internal.Record.class);

        byte[] bytes = "my-value".getBytes(StandardCharsets.UTF_8);
        Header[] headers = { new RecordHeader("my-key", bytes) };
        when(mockRecord.headers()).thenReturn(headers);

        List<Object> result = new ArrayList<>();
        new PathEvaluator<>(new KafkaRecordTree()).eval(mockRecord, new Path<>(Identifier.ROOT,
                List.of(new Segment.Child<>(new Selector.Name<>("headers")),
                        new Segment.Child<>(new Selector.Children<>()),
                        new Segment.Child<>(new Selector.Name<>("value"))), result::add));
        assertThat(result).singleElement().isSameAs(bytes);

        //Mockito.verifyNoMoreInteractions(mockRecord);
    }

}
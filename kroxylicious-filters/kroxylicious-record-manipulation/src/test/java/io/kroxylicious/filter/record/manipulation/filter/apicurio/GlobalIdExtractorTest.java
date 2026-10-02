/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.record.manipulation.filter.apicurio;

import java.nio.ByteBuffer;

import org.junit.jupiter.api.Test;

import io.kroxylicious.filter.record.manipulation.filter.GlobalId;
import io.kroxylicious.kafka.common.header.Header;
import io.kroxylicious.kafka.common.record.internal.Record;
import io.kroxylicious.testing.filter.record.RecordTestUtils;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;

class GlobalIdExtractorTest {

    private static final Header[] NO_HEADERS = new Header[0];

    private static byte[] framed(long globalId) {
        return ByteBuffer.allocate(1 + Long.BYTES).put((byte) 0x00).putLong(globalId).array();
    }

    @Test
    void extractsGlobalIdFromFramedKey() {
        // Given
        Record record = RecordTestUtils.record(framed(4242L), "value".getBytes(UTF_8), NO_HEADERS);

        // When
        GlobalId globalId = new GlobalIdExtractor(Record::key).extract(record);

        // Then
        assertThat(globalId).isEqualTo(new GlobalId(4242L));
    }

    @Test
    void extractsGlobalIdFromFramedValue() {
        // Given
        Record record = RecordTestUtils.record("key".getBytes(UTF_8), framed(7L), NO_HEADERS);

        // When
        GlobalId globalId = new GlobalIdExtractor(Record::value).extract(record);

        // Then
        assertThat(globalId).isEqualTo(new GlobalId(7L));
    }

    @Test
    void returnsNullWhenMagicByteAbsent() {
        // Given
        byte[] notFramed = ByteBuffer.allocate(1 + Long.BYTES).put((byte) 0x01).putLong(9L).array();
        Record record = RecordTestUtils.record(notFramed, "value".getBytes(UTF_8), NO_HEADERS);

        // When
        GlobalId globalId = new GlobalIdExtractor(Record::key).extract(record);

        // Then
        assertThat(globalId).isNull();
    }

    @Test
    void returnsNullWhenBufferTooShort() {
        // Given
        Record record = RecordTestUtils.record(new byte[]{ 0x00, 1, 2 }, "value".getBytes(UTF_8), NO_HEADERS);

        // When
        GlobalId globalId = new GlobalIdExtractor(Record::key).extract(record);

        // Then
        assertThat(globalId).isNull();
    }

    @Test
    void returnsNullWhenKeyAbsent() {
        // Given
        Record record = RecordTestUtils.record("value".getBytes(UTF_8), NO_HEADERS);

        // When
        GlobalId globalId = new GlobalIdExtractor(Record::key).extract(record);

        // Then
        assertThat(globalId).isNull();
    }
}

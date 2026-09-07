/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.record.manipulation.format.avro;

import java.nio.ByteBuffer;

import org.apache.avro.Schema;
import org.apache.avro.generic.GenericData;
import org.apache.avro.generic.GenericRecord;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AvroJsonSerializerTest {

    private static final Schema SCHEMA = new Schema.Parser()
            .parse("{\"type\":\"record\",\"name\":\"Greeting\",\"fields\":[{\"name\":\"text\",\"type\":\"string\"}]}");

    private final AvroJsonSerializer serializer = new AvroJsonSerializer(SCHEMA);

    private static GenericRecord record(String text) {
        GenericRecord record = new GenericData.Record(SCHEMA);
        record.put("text", text);
        return record;
    }

    @Test
    void returnsABufferReadyToBeRead() {
        // Given
        GenericRecord record = record("hello");

        // When
        ByteBuffer buffer = serializer.apply(record);

        // Then
        assertThat(buffer.position()).isZero();
        assertThat(buffer.remaining()).isGreaterThan(0);
    }

    @Test
    void deserializingASerializedRecordReturnsAnEquivalentRecord() {
        // Given
        AvroJsonDeserializer deserializer = new AvroJsonDeserializer(SCHEMA);
        GenericRecord record = record("hello");

        // When
        ByteBuffer buffer = serializer.apply(record);
        GenericRecord roundTripped = deserializer.apply(buffer);

        // Then
        assertThat(roundTripped.get("text").toString()).isEqualTo("hello");
    }

}

/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.record.manipulation.avro;

import java.nio.ByteBuffer;

import org.apache.avro.Schema;
import org.apache.avro.generic.GenericData;
import org.apache.avro.generic.GenericRecord;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AvroBinaryDeserializerTest {

    private static final Schema SCHEMA = new Schema.Parser()
            .parse("{\"type\":\"record\",\"name\":\"Greeting\",\"fields\":[{\"name\":\"text\",\"type\":\"string\"}]}");

    private final AvroBinarySerializer serializer = new AvroBinarySerializer(SCHEMA);
    private final AvroBinaryDeserializer deserializer = new AvroBinaryDeserializer(SCHEMA);

    private static GenericRecord record(String text) {
        GenericRecord record = new GenericData.Record(SCHEMA);
        record.put("text", text);
        return record;
    }

    @Test
    void deserializesAnArrayBackedBuffer() {
        // Given
        ByteBuffer buffer = serializer.apply(record("hello"));

        // When
        GenericRecord result = deserializer.apply(buffer);

        // Then
        assertThat(result.get("text").toString()).isEqualTo("hello");
    }

    @Test
    void deserializesOnlyTheRemainingBytesOfASlicedArrayBackedBuffer() {
        // Given
        ByteBuffer encoded = serializer.apply(record("hello"));
        int length = encoded.remaining();
        byte[] padded = new byte[5 + length + 5];
        encoded.get(padded, 5, length);
        ByteBuffer buffer = ByteBuffer.wrap(padded);
        buffer.position(5);
        buffer.limit(5 + length);
        ByteBuffer slice = buffer.slice();

        // When
        GenericRecord result = deserializer.apply(slice);

        // Then
        assertThat(result.get("text").toString()).isEqualTo("hello");
    }

    @Test
    void deserializesANonArrayBackedBuffer() {
        // Given
        ByteBuffer encoded = serializer.apply(record("hello"));
        ByteBuffer buffer = ByteBuffer.allocateDirect(encoded.remaining());
        buffer.put(encoded).flip();

        // When
        GenericRecord result = deserializer.apply(buffer);

        // Then
        assertThat(result.get("text").toString()).isEqualTo("hello");
    }

    @Test
    void wrapsIOExceptionFromInvalidAvroInARuntimeException() {
        // Given
        ByteBuffer buffer = ByteBuffer.wrap(new byte[]{ (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF });

        // When/Then
        assertThatThrownBy(() -> deserializer.apply(buffer))
                .isInstanceOf(RuntimeException.class);
    }

}

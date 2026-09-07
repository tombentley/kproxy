/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.record.manipulation.format.avro;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

import org.apache.avro.Schema;
import org.apache.avro.generic.GenericData;
import org.apache.avro.generic.GenericRecord;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AvroJsonDeserializerTest {

    private static final Schema SCHEMA = new Schema.Parser()
            .parse("{\"type\":\"record\",\"name\":\"Greeting\",\"fields\":[{\"name\":\"text\",\"type\":\"string\"}]}");

    private final AvroJsonSerializer serializer = new AvroJsonSerializer(SCHEMA);
    private final AvroJsonDeserializer deserializer = new AvroJsonDeserializer(SCHEMA);

    private static GenericRecord record(String text) {
        GenericRecord record = new GenericData.Record(SCHEMA);
        record.put("text", text);
        return record;
    }

    @Test
    void deserializesABuffer() {
        // Given
        ByteBuffer buffer = serializer.apply(record("hello"));

        // When
        GenericRecord result = deserializer.apply(buffer);

        // Then
        assertThat(result.get("text").toString()).isEqualTo("hello");
    }

    @Test
    void wrapsIOExceptionFromInvalidAvroJsonInARuntimeException() {
        // Given
        ByteBuffer buffer = ByteBuffer.wrap("not avro json".getBytes(StandardCharsets.UTF_8));

        // When/Then
        assertThatThrownBy(() -> deserializer.apply(buffer))
                .isInstanceOf(RuntimeException.class);
    }

}

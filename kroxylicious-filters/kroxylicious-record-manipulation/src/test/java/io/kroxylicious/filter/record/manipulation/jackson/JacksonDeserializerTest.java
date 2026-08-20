/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.record.manipulation.jackson;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.TextNode;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JacksonDeserializerTest {

    private final ObjectMapper mapper = new ObjectMapper();
    private final JacksonDeserializer deserializer = new JacksonDeserializer(mapper);

    @Test
    void deserializesAnArrayBackedBuffer() {
        // Given
        ByteBuffer buffer = ByteBuffer.wrap("\"hello\"".getBytes(StandardCharsets.UTF_8));

        // When
        JsonNode node = deserializer.apply(buffer);

        // Then
        assertThat(node).isEqualTo(new TextNode("hello"));
    }

    @Test
    void deserializesOnlyTheRemainingBytesOfASlicedArrayBackedBuffer() {
        // Given
        byte[] bytes = "XXXXX\"hello\"YYYYY".getBytes(StandardCharsets.UTF_8);
        ByteBuffer buffer = ByteBuffer.wrap(bytes);
        buffer.position(5);
        buffer.limit(bytes.length - 5);
        ByteBuffer slice = buffer.slice();

        // When
        JsonNode node = deserializer.apply(slice);

        // Then
        assertThat(node).isEqualTo(new TextNode("hello"));
    }

    @Test
    void deserializesANonArrayBackedBuffer() {
        // Given
        byte[] bytes = "\"hello\"".getBytes(StandardCharsets.UTF_8);
        ByteBuffer buffer = ByteBuffer.allocateDirect(bytes.length);
        buffer.put(bytes).flip();

        // When
        JsonNode node = deserializer.apply(buffer);

        // Then
        assertThat(node).isEqualTo(new TextNode("hello"));
    }

    @Test
    void wrapsIOExceptionFromInvalidJsonInARuntimeException() {
        // Given
        ByteBuffer buffer = ByteBuffer.wrap("not json".getBytes(StandardCharsets.UTF_8));

        // When/Then
        assertThatThrownBy(() -> deserializer.apply(buffer))
                .isInstanceOf(RuntimeException.class);
    }

}

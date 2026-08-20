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
import com.fasterxml.jackson.databind.node.IntNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;

import static org.assertj.core.api.Assertions.assertThat;

class JacksonSerializerTest {

    private final ObjectMapper mapper = new ObjectMapper();
    private final JacksonSerializer serializer = new JacksonSerializer(mapper);

    @Test
    void returnsABufferReadyToBeRead() {
        // Given
        TextNode node = new TextNode("hello");

        // When
        ByteBuffer buffer = serializer.apply(node);

        // Then
        assertThat(buffer.position()).isZero();
        assertThat(buffer.remaining()).isGreaterThan(0);
    }

    @Test
    void serializesATextNodeToItsJsonRepresentation() {
        // Given
        TextNode node = new TextNode("hello");

        // When
        ByteBuffer buffer = serializer.apply(node);

        // Then
        assertThat(StandardCharsets.UTF_8.decode(buffer).toString()).isEqualTo("\"hello\"");
    }

    @Test
    void serializesAnObjectNodeToItsJsonRepresentation() {
        // Given
        ObjectNode node = JsonNodeFactory.instance.objectNode();
        node.set("a", new IntNode(1));

        // When
        ByteBuffer buffer = serializer.apply(node);

        // Then
        assertThat(StandardCharsets.UTF_8.decode(buffer).toString()).isEqualTo("{\"a\":1}");
    }

    @Test
    void deserializingASerializedNodeReturnsAnEquivalentNode() {
        // Given
        JacksonDeserializer deserializer = new JacksonDeserializer(mapper);
        ObjectNode node = JsonNodeFactory.instance.objectNode();
        node.set("a", new IntNode(1));
        node.set("b", new TextNode("hello"));

        // When
        ByteBuffer buffer = serializer.apply(node);
        JsonNode roundTripped = deserializer.apply(buffer);

        // Then
        assertThat(roundTripped).isEqualTo(node);
    }

}

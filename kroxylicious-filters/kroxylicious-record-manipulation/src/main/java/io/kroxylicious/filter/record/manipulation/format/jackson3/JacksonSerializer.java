/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.record.manipulation.format.jackson3;

import java.nio.ByteBuffer;
import java.util.function.Function;

import org.apache.kafka.common.utils.ByteBufferOutputStream;

import io.kroxylicious.filter.record.manipulation.format.SerializationException;
import io.kroxylicious.filter.record.manipulation.format.Serializer;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectWriter;

/**
 * Serializes a {@link JsonNode} to a {@link ByteBuffer} ready to be read.
 */
public class JacksonSerializer implements Function<JsonNode, ByteBuffer>, Serializer<JsonNode> {

    private final ObjectWriter writer;

    /**
     * Creates a serializer.
     * @param writer the mapper used to write the node's contents
     */
    public JacksonSerializer(ObjectWriter writer) {
        this.writer = writer;
    }

    @Override
    public ByteBuffer apply(JsonNode node) {
        return serialize(node);
    }

    @Override
    public ByteBuffer serialize(JsonNode node) {
        // TODO buffer recycling
        try (var is = new ByteBufferOutputStream(10000)) {
            writer.writeValue(is, node);
            ByteBuffer buffer = is.buffer();
            buffer.flip();
            return buffer;
        }
        catch (Exception e) {
            throw new SerializationException(e);
        }
    }
}

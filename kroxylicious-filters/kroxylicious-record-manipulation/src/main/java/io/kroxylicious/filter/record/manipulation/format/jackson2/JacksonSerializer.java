/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.record.manipulation.format.jackson2;

import java.nio.ByteBuffer;
import java.util.function.Function;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.kroxylicious.filter.record.manipulation.format.SerializationException;
import io.kroxylicious.filter.record.manipulation.format.Serializer;
import io.kroxylicious.kafka.common.utils.ByteBufferOutputStream;

/**
 * Serializes a {@link JsonNode} to a {@link ByteBuffer} ready to be read.
 */
public class JacksonSerializer implements Function<JsonNode, ByteBuffer>, Serializer<JsonNode> {

    private final ObjectMapper mapper;

    /**
     * Creates a serializer.
     * @param mapper the mapper used to write the node's contents
     */
    public JacksonSerializer(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public ByteBuffer apply(JsonNode node) {
        return serialize(node);
    }

    @Override
    public ByteBuffer serialize(JsonNode node) {
        // TODO buffer recycling
        try (var is = new ByteBufferOutputStream(10000)) {
            mapper.writeValue(is, node);
            ByteBuffer buffer = is.buffer();
            buffer.flip();
            return buffer;
        }
        catch (Exception e) {
            throw new SerializationException(e);
        }
    }
}

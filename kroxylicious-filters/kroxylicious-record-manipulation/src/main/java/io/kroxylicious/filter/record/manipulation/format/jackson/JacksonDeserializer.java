/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.record.manipulation.format.jackson;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.function.Function;

import org.apache.kafka.common.utils.ByteBufferInputStream;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.kroxylicious.filter.record.manipulation.format.DeserializationException;
import io.kroxylicious.filter.record.manipulation.format.Deserializer;

/**
 * Deserializes the remaining bytes of a {@link ByteBuffer} to a {@link JsonNode}.
 */
public class JacksonDeserializer implements Function<ByteBuffer, JsonNode>, Deserializer<JsonNode> {

    private final ObjectMapper mapper;

    /**
     * Creates a deserializer.
     * @param mapper the mapper used to parse the buffer's contents
     */
    public JacksonDeserializer(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public JsonNode apply(ByteBuffer bb) {
        return deserialize(bb);
    }

    @Override
    public JsonNode deserialize(ByteBuffer byteBuffer) {
        try {
            if (byteBuffer.hasArray()) {
                return mapper.readTree(byteBuffer.array(), byteBuffer.arrayOffset(), byteBuffer.remaining());
            }
            else {
                try (var is = new ByteBufferInputStream(byteBuffer)) {
                    return mapper.readTree(is);
                }
            }
        }
        catch (Exception e) {
            throw new DeserializationException(e);
        }
    }
}

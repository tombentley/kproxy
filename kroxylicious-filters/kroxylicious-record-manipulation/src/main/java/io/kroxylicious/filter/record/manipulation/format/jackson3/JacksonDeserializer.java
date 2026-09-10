/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.record.manipulation.format.jackson3;

import java.nio.ByteBuffer;
import java.util.function.Function;

import org.apache.kafka.common.utils.ByteBufferInputStream;

import io.kroxylicious.filter.record.manipulation.format.DeserializationException;
import io.kroxylicious.filter.record.manipulation.format.Deserializer;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectReader;

/**
 * Deserializes the remaining bytes of a {@link ByteBuffer} to a {@link JsonNode}.
 */
public class JacksonDeserializer implements Function<ByteBuffer, JsonNode>, Deserializer<JsonNode> {

    private final ObjectReader reader;

    /**
     * Creates a deserializer.
     * @param reader the mapper used to parse the buffer's contents
     */
    public JacksonDeserializer(ObjectReader reader) {
        this.reader = reader;
    }

    @Override
    public JsonNode apply(ByteBuffer bb) {
        return deserialize(bb);
    }

    @Override
    public JsonNode deserialize(ByteBuffer byteBuffer) {
        try {
            if (byteBuffer.hasArray()) {
                return reader.readTree(byteBuffer.array(), byteBuffer.arrayOffset(), byteBuffer.remaining());
            }
            else {
                try (var is = new ByteBufferInputStream(byteBuffer)) {
                    return reader.readTree(is);
                }
            }
        }
        catch (Exception e) {
            throw new DeserializationException(e);
        }
    }
}

/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.record.manipulation.format.jackson3;

import java.nio.ByteBuffer;
import java.util.function.Function;

import io.kroxylicious.filter.record.manipulation.format.DeserializationException;
import io.kroxylicious.filter.record.manipulation.format.Deserializer;
import io.kroxylicious.kafka.common.utils.ByteBufferInputStream;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectReader;

/**
 * Deserializes the remaining bytes of a {@link ByteBuffer} to a {@link JsonNode}.
 */
class JacksonDeserializer {

    private final ObjectReader reader;

    /**
     * Creates a deserializer.
     * @param reader the mapper used to parse the buffer's contents
     */
    JacksonDeserializer(ObjectReader reader) {
        this.reader = reader;
    }

    Object deserialize(ByteBuffer byteBuffer) {
        try {
            if (byteBuffer.hasArray()) {
                return reader.readValue(byteBuffer.array(), byteBuffer.arrayOffset(), byteBuffer.remaining());
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

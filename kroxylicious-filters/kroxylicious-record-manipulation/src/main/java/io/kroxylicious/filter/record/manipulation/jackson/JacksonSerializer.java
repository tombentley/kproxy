/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.record.manipulation.jackson;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.util.function.Function;

import org.apache.kafka.common.utils.ByteBufferOutputStream;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class JacksonSerializer implements Function<JsonNode, ByteBuffer> {

    private final ObjectMapper mapper;

    public JacksonSerializer(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public ByteBuffer apply(JsonNode node) {
        // TODO buffer recycling
        try (var is = new ByteBufferOutputStream(10000)) {
            mapper.writeValue(is, node);
            return is.buffer();
        }
        catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}

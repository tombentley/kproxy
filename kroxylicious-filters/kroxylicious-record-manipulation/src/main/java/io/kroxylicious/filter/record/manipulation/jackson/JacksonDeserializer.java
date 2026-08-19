/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.record.manipulation.jackson;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.function.Function;

import org.apache.kafka.common.utils.ByteBufferInputStream;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class JacksonDeserializer implements Function<ByteBuffer, JsonNode> {

    private final ObjectMapper mapper;
    public JacksonDeserializer(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public JsonNode apply(ByteBuffer bb) {
        try {
            if (bb.hasArray()) {
                return mapper.readTree(bb.array(), bb.arrayOffset(), bb.remaining());
            }
            else {
                try (var is = new ByteBufferInputStream(bb)) {
                    return mapper.readTree(is);
                }
            }
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}

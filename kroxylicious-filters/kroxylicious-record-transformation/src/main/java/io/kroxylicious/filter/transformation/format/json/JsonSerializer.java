/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.transformation.format.json;

import java.io.IOException;
import java.io.OutputStream;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.kroxylicious.filter.transformation.api.TypeException;
import io.kroxylicious.filter.transformation.api.Type;
import io.kroxylicious.filter.transformation.api.format.Serializer;

public class JsonSerializer implements Serializer<JsonNode> {

    private final ObjectMapper mapper;

    public JsonSerializer(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public void accepts(Type<?, ?, ?> type) {
        if (type.cls() != JsonNode.class) {
            throw new TypeException("");
        }
    }

    @Override
    public void serialize(JsonNode value, OutputStream out) throws IOException {
        mapper.writeValue(out, value);
    }
}

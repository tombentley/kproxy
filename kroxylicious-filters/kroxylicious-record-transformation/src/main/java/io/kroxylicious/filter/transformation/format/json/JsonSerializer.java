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

import io.kroxylicious.filter.transformation.api.format.Serializer;

public class JsonSerializer implements
        Serializer<JsonNode> {

    static final ObjectMapper MAPPER = new ObjectMapper();
    public static final JsonSerializer INSTANCE = new JsonSerializer();

    @Override
    public Class<JsonNode> acceptedType() {
        return JsonNode.class;
    }

    @Override
    public void serialize(JsonNode value, OutputStream out) throws IOException {
        MAPPER.writeValue(out, value);
    }
}

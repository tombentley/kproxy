/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.transformation.format.json;

import java.io.IOException;
import java.io.InputStream;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.kroxylicious.filter.transformation.api.format.Deserializer;
import io.kroxylicious.filter.transformation.api.mapper.Context;

public class JsonDeserializer implements
        Deserializer<JsonNode> {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    public static final JsonDeserializer INSTANCE = new JsonDeserializer();

    @Override
    public JsonNode deserialize(InputStream in, Context context) throws IOException {
        var source = MAPPER.readTree(in);
        return source;
    }

    @Override
    public Class<JsonNode> returnedType() {
        return JsonNode.class;
    }

}

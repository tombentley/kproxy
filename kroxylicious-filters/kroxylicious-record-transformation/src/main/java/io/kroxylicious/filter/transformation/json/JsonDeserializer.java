/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.transformation.json;

import java.io.IOException;
import java.io.InputStream;

import org.apache.kafka.common.header.Header;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.kroxylicious.filter.transformation.api.format.Deserializer;

public class JsonDeserializer implements
        Deserializer<JsonNode> {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Override
    public JsonNode deserialize(Header[] headers, InputStream in) throws IOException {
        var source = MAPPER.readTree(in);
        return source;
    }

    @Override
    public Class<JsonNode> returnedType() {
        return JsonNode.class;
    }

}

/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.transformation.aafresh.json;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.kroxylicious.filter.transformation.aafresh.Ser;

public class JsonSer implements Ser<JsonNode> {

    private final ObjectMapper mapper;

    public JsonSer(ObjectMapper mapper) {
        this.mapper = mapper;
    }
    @Override
    public void serialize(JsonNode value, OutputStream out) throws IOException {
        return mapper.writeValue(out, value);
    }
}

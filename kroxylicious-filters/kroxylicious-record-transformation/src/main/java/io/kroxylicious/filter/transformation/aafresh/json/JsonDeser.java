/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.transformation.aafresh.json;

import java.io.InputStream;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.kroxylicious.filter.transformation.aafresh.Deser;

public class JsonDeser implements Deser<JsonNode> {
    private final ObjectMapper mapper;

    public JsonDeser(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public JsonNode deser(InputStream inputStream) {
        return mapper.readTree(inputStream);
    }
}

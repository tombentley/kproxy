/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.transformation.format.json;

import java.io.IOException;

import com.fasterxml.jackson.databind.JsonNode;

import io.kroxylicious.filter.transformation.api.format.DataFormat;
import io.kroxylicious.filter.transformation.api.format.Deserializer;
import io.kroxylicious.filter.transformation.api.format.Serializer;

public class JsonFormat implements DataFormat<JsonNode> {
    @Override
    public Class<JsonNode> type() {
        return JsonNode.class;
    }

    @Override
    public Serializer<JsonNode> serializer() {
        return new JsonSerializer();
    }

    @Override
    public Deserializer<JsonNode> deserializer() {
        return new JsonDeserializer();
    }
}

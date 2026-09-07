/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.record.manipulation.jackson;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.kroxylicious.filter.record.manipulation.format.DataFormat;
import io.kroxylicious.filter.record.manipulation.format.Deserializer;
import io.kroxylicious.filter.record.manipulation.format.Serializer;

public class JacksonJsonFormat implements DataFormat<JsonNode> {

    private final ObjectMapper mapper = new ObjectMapper();

    @Override
    public Serializer<JsonNode> serializer() {
        return new JacksonSerializer(mapper);
    }

    @Override
    public Deserializer<JsonNode> deserializer() {
        return new JacksonDeserializer(mapper);
    }
}

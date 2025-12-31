/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.transformation.format.json;

import java.util.Set;

import org.apache.kafka.shaded.com.google.protobuf.DescriptorProtos;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.kroxylicious.filter.transformation.api.format.DataFormat;
import io.kroxylicious.filter.transformation.api.format.Deserializer;
import io.kroxylicious.filter.transformation.api.format.Serializer;
import io.kroxylicious.filter.transformation.api.schema.identification.NoSchemaId;
import io.kroxylicious.filter.transformation.api.schema.identification.WireSchemaId;

public class JsonFormat implements DataFormat<Void, JsonNode> {

    public static final JsonFormat INSTANCE = new JsonFormat();

    private final ObjectMapper mapper = new ObjectMapper();

    @Override
    public String defaultEncoding() {
        return "json";
    }

    @Override
    public Set<String> encodings() {
        return Set.of(defaultEncoding());
    }

    @Override
    public WireSchemaId schemaId() {
        return NoSchemaId.INSTANCE;
    }

    @Override
    public Serializer<JsonNode> serializer(String encoding) {
        return new JsonSerializer(mapper);
    }

    @Override
    public Deserializer<Void, JsonNode> deserializer(String encoding) {
        return new JsonDeserializer(mapper);
    }
}

/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.transformation.format.json;

import java.util.Set;

import org.apache.kafka.shaded.com.google.protobuf.DescriptorProtos;

import com.fasterxml.jackson.databind.JsonNode;

import io.kroxylicious.filter.transformation.api.format.DataFormat;
import io.kroxylicious.filter.transformation.api.format.Deserializer;
import io.kroxylicious.filter.transformation.api.format.Serializer;
import io.kroxylicious.filter.transformation.api.schema.identification.NoSchemaId;
import io.kroxylicious.filter.transformation.api.schema.identification.WireSchemaId;

public class JsonFormat implements DataFormat<JsonFormat.Encoding, Void, JsonNode> {
    public static final JsonFormat INSTANCE = new JsonFormat();
    public enum Encoding {}

    @Override
    public Set<Encoding> encodings() {
        return Set.of();
    }

    @Override
    public WireSchemaId schemaId() {
        return NoSchemaId.INSTANCE;
    }

    @Override
    public Serializer<JsonNode> serializer(Encoding encoding) {
        return new JsonSerializer();
    }

    @Override
    public Deserializer<Void, JsonNode> deserializer(Encoding encoding) {
        return new JsonDeserializer();
    }
}

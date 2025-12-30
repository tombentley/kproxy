/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.transformation.format.json;

import java.io.IOException;
import java.util.Optional;

import com.fasterxml.jackson.databind.JsonNode;

import io.kroxylicious.filter.transformation.api.format.DataFormat;
import io.kroxylicious.filter.transformation.api.format.Deserializer;
import io.kroxylicious.filter.transformation.api.format.Serializer;
import io.kroxylicious.filter.transformation.api.schema.identification.NoSchema;
import io.kroxylicious.filter.transformation.api.schema.identification.WireSchemaId;

public class JsonFormat implements DataFormat<JsonNode> {
    @Override
    public WireSchemaId schemaId() {
        return NoSchema.INSTANCE;
    }

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

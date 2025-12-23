/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.transformation.json;

import java.io.IOException;

import org.apache.kafka.common.header.Header;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.kroxylicious.filter.transformation.Datum;
import io.kroxylicious.filter.transformation.DatumDeserializer;
import io.kroxylicious.filter.transformation.NoSchema;
import io.kroxylicious.filter.transformation.TransformationInputStream;

public class JsonDeserializer implements
        DatumDeserializer<JsonNode> {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Override
    public Datum<JsonNode> deserialize(Header[] headers, TransformationInputStream in) throws IOException {
        var source = MAPPER.readTree(in);
        return new Datum<>(NoSchema.INSTANCE, JsonNode.class, source);
    }

    @Override
    public Class<JsonNode> returnedType() {
        return JsonNode.class;
    }

}

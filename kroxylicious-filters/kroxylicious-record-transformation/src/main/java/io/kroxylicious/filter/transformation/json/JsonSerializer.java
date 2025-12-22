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
import io.kroxylicious.filter.transformation.DatumSerializer;
import io.kroxylicious.filter.transformation.TransformationInputStream;
import io.kroxylicious.filter.transformation.TransformationOutputStream;

public class JsonSerializer implements
        DatumSerializer<JsonNode> {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Override
    public Class<JsonNode> acceptedType() {
        return JsonNode.class;
    }

    @Override
    public void serialize(Datum<JsonNode> datum, TransformationOutputStream out) throws IOException {
        MAPPER.writeValue(out, datum.datum());
    }
}

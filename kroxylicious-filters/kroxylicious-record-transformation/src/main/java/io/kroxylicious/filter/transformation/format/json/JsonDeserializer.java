/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.transformation.format.json;

import java.io.IOException;
import java.io.InputStream;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.kroxylicious.filter.transformation.api.TypeException;
import io.kroxylicious.filter.transformation.api.SchemaAndValue;
import io.kroxylicious.filter.transformation.api.Type;
import io.kroxylicious.filter.transformation.api.format.Deserializer;
import io.kroxylicious.filter.transformation.api.mapper.Context;
import io.kroxylicious.filter.transformation.api.schema.identification.NoSchema;

public class JsonDeserializer implements
        Deserializer<NoSchema, JsonNode> {

    @Override
    public Type<?, ?, ?> typeCheck(Type<?, ?, ?> type) {
        if (!InputStream.class.isAssignableFrom(type.cls())) {
            throw new TypeException(String.format("Type %s is not assignable to InputStream", type));
        }
        return new Type(NoSchema.class, null, JsonNode.class);
    }

    private static final ObjectMapper MAPPER = new ObjectMapper();
    public static final JsonDeserializer INSTANCE = new JsonDeserializer();

    @Override
    public SchemaAndValue<NoSchema, JsonNode> deserialize(InputStream in, Context context) throws IOException {
        var source = MAPPER.readTree(in);
        return new SchemaAndValue<>(NoSchema.INSTANCE, source);
    }

}

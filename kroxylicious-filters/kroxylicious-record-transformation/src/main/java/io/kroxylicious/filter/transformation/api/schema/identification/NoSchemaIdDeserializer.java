/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.transformation.api.schema.identification;

import java.io.IOException;
import java.io.InputStream;

import io.kroxylicious.filter.transformation.TransformationInputStream;
import io.kroxylicious.filter.transformation.api.SchemaAndValue;
import io.kroxylicious.filter.transformation.api.Type;
import io.kroxylicious.filter.transformation.api.TypeException;
import io.kroxylicious.filter.transformation.api.mapper.Context;

public class NoSchemaIdDeserializer implements SchemaIdDeserializer<NoSchemaId> {
    public static final NoSchemaIdDeserializer INSTANCE = new NoSchemaIdDeserializer();

    @Override
    public SchemaAndValue<NoSchemaId, Void, InputStream> deserialize(InputStream stream, Context context) throws IOException {
        return new SchemaAndValue<>(NoSchemaId.INSTANCE, null, stream);
    }

    @Override
    public Type<?, ?, ?> typeCheck(Type<?, ?, ?> type) {
        if (!TransformationInputStream.class.isAssignableFrom(type.cls())) {
            throw new TypeException(String.format("Type %s is not assignable to InputStream", type));
        }
        return new Type<>(NoSchemaId.class, Void.class, TransformationInputStream.class);
    }
}

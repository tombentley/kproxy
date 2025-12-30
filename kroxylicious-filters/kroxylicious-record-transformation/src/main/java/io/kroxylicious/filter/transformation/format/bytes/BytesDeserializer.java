/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.transformation.format.bytes;

import java.io.IOException;
import java.io.InputStream;

import io.kroxylicious.filter.transformation.TransformationInputStream;
import io.kroxylicious.filter.transformation.api.TypeException;
import io.kroxylicious.filter.transformation.api.SchemaAndValue;
import io.kroxylicious.filter.transformation.api.Type;
import io.kroxylicious.filter.transformation.api.format.Deserializer;
import io.kroxylicious.filter.transformation.api.mapper.Context;
import io.kroxylicious.filter.transformation.api.schema.identification.NoSchema;

public class BytesDeserializer implements Deserializer<NoSchema, TransformationInputStream> {

    public static final BytesDeserializer INSTANCE = new BytesDeserializer();

    @Override
    public Type<?, ?, ?> typeCheck(Type<?, ?, ?> type) {
        if (!TransformationInputStream.class.isAssignableFrom(type.cls())) {
            throw new TypeException(String.format("Type %s is not assignable to InputStream", type));
        }
        return new Type(NoSchema.class, null, TransformationInputStream.class);
    }

    @Override
    public SchemaAndValue<NoSchema, TransformationInputStream> deserialize(InputStream in, Context context) throws IOException {
        return new SchemaAndValue<>(NoSchema.INSTANCE, (TransformationInputStream) in);
    }

}

/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.transformation.api.schema.identification;

import java.io.IOException;
import java.io.InputStream;

import io.kroxylicious.filter.transformation.api.format.Deserializer;
import io.kroxylicious.filter.transformation.api.mapper.Context;

public class NoSchemaIdDeserializer implements Deserializer<NoSchema> {
    public static final NoSchemaIdDeserializer INSTANCE = new NoSchemaIdDeserializer();
    @Override
    public Class<NoSchema> returnedType() {
        return NoSchema.class;
    }

    @Override
    public NoSchema deserialize(InputStream data, Context context) throws IOException {
        return NoSchema.INSTANCE;
    }

}

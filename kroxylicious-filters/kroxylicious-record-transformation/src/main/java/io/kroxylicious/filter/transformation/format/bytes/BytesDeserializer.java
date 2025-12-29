/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.transformation.format.bytes;

import java.io.IOException;
import java.io.InputStream;

import io.kroxylicious.filter.transformation.TransformationInputStream;
import io.kroxylicious.filter.transformation.api.format.Deserializer;
import io.kroxylicious.filter.transformation.api.mapper.Context;

public class BytesDeserializer implements Deserializer<TransformationInputStream> {

    public static final BytesDeserializer INSTANCE = new BytesDeserializer();

    @Override
    public TransformationInputStream deserialize(InputStream in, Context context) throws IOException {
        return (TransformationInputStream) in;
    }

    @Override
    public Class<TransformationInputStream> returnedType() {
        return TransformationInputStream.class;
    }
}

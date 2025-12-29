/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.transformation.format.bytes;

import java.io.IOException;

import io.kroxylicious.filter.transformation.TransformationInputStream;
import io.kroxylicious.filter.transformation.api.format.DataFormat;
import io.kroxylicious.filter.transformation.api.format.Deserializer;
import io.kroxylicious.filter.transformation.api.format.Serializer;

public class BytesFormat implements DataFormat<TransformationInputStream> {
    @Override
    public Class<TransformationInputStream> type() {
        return TransformationInputStream.class;
    }

    @Override
    public Serializer<TransformationInputStream> serializer() {
        return new BytesSerializer();
    }

    @Override
    public Deserializer<TransformationInputStream> deserializer() {
        return new BytesDeserializer();
    }
}

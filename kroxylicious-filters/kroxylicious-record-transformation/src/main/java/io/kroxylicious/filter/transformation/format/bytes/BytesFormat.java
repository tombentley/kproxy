/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.transformation.format.bytes;

import java.util.Set;

import io.kroxylicious.filter.transformation.TransformationInputStream;
import io.kroxylicious.filter.transformation.api.format.DataFormat;
import io.kroxylicious.filter.transformation.api.format.Deserializer;
import io.kroxylicious.filter.transformation.api.format.Serializer;
import io.kroxylicious.filter.transformation.api.schema.identification.NoSchemaId;
import io.kroxylicious.filter.transformation.api.schema.identification.WireSchemaId;

public class BytesFormat implements DataFormat<Void, TransformationInputStream> {

    public static final BytesFormat INSTANCE = new BytesFormat();

    @Override
    public String defaultEncoding() {
        return "binary";
    }

    @Override
    public Set<String> encodings() {
        return Set.of(defaultEncoding());
    }

    @Override
    public WireSchemaId schemaId() {
        return NoSchemaId.INSTANCE;
    }

    @Override
    public Serializer<TransformationInputStream> serializer(String encoding) {
        return new BytesSerializer();
    }

    @Override
    public Deserializer<Void, TransformationInputStream> deserializer(String encoding) {
        return new BytesDeserializer();
    }

}

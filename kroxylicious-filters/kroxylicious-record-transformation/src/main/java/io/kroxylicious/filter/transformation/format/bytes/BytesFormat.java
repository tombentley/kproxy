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

import edu.umd.cs.findbugs.annotations.Nullable;

public class BytesFormat implements DataFormat<BytesFormat.Encoding, Void, TransformationInputStream> {

    public static final BytesFormat INSTANCE = new BytesFormat();

    public enum Encoding {}

    @Override
    public Set<Encoding> encodings() {
        return Set.of();
    }

    @Override
    public WireSchemaId schemaId() {
        return NoSchemaId.INSTANCE;
    }

    @Override
    public Serializer<TransformationInputStream> serializer(@Nullable Encoding encoding) {
        return new BytesSerializer();
    }

    @Override
    public Deserializer<Void, TransformationInputStream> deserializer(@Nullable Encoding encoding) {
        return new BytesDeserializer();
    }

}

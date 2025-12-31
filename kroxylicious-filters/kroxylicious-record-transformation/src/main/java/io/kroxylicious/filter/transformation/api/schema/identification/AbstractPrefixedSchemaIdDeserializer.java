/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.transformation.api.schema.identification;

import java.io.IOException;
import java.io.InputStream;

import io.kroxylicious.filter.transformation.TransformationInputStream;
import io.kroxylicious.filter.transformation.api.TypeException;
import io.kroxylicious.filter.transformation.api.SchemaAndValue;
import io.kroxylicious.filter.transformation.api.Type;
import io.kroxylicious.filter.transformation.api.mapper.Context;

public abstract class AbstractPrefixedSchemaIdDeserializer
        implements SchemaIdDeserializer<ByteWireId> {

    private final int magic;
    private final int prefixLengthWithMagic;

    @Override
    public Type<ByteWireId, ?, ?> typeCheck(Type<?, ?, ?> type) {
        if (!TransformationInputStream.class.isAssignableFrom(type.cls())) {
            throw new TypeException(String.format("Type %s is not assignable to InputStream", type));
        }
        return new Type<>(ByteWireId.class, Void.class, TransformationInputStream.class);
    }

    AbstractPrefixedSchemaIdDeserializer(int magic, int prefixLengthWithMagic) {
        this.magic = magic;
        this.prefixLengthWithMagic = prefixLengthWithMagic;
    }

    @Override
    public SchemaAndValue<ByteWireId, Void, InputStream> deserialize(InputStream stream, Context context) throws IOException {
        stream.mark(1);
        int maybeMagic = stream.read();
        stream.reset();
        if (maybeMagic == magic && stream.available() >= prefixLengthWithMagic) {
            // Note that we intentionally include the magic byte.
            return new SchemaAndValue<>(new ByteWireId(stream.readNBytes(prefixLengthWithMagic)), null, stream);
        }
        return null;
    }

}

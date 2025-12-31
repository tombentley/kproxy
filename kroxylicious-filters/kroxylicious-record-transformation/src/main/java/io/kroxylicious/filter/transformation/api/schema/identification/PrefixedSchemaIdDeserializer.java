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

public abstract class PrefixedSchemaIdDeserializer<W extends WireSchemaId>
        implements SchemaIdDeserializer<W> {

    private final byte magic;
    private final int prefixLengthAfterMagic;

    abstract Class<W> wireSchemaIdClass();

    abstract W parse(byte[] bytes);

    @Override
    public Type<W, ?, ?> typeCheck(Type<?, ?, ?> type) {
        if (!TransformationInputStream.class.isAssignableFrom(type.cls())) {
            throw new TypeException(String.format("Type %s is not assignable to InputStream", type));
        }
        return new Type<>(wireSchemaIdClass(), Void.class, TransformationInputStream.class);
    }

    PrefixedSchemaIdDeserializer(byte magic, int prefixLengthAfterMagic) {
        this.magic = magic;
        this.prefixLengthAfterMagic = prefixLengthAfterMagic;
    }

    @Override
    public SchemaAndValue<W, Void, InputStream> deserialize(InputStream stream, Context context) throws IOException {
        stream.mark(1);
        int maybeMagic = stream.read();
        if (maybeMagic == magic && stream.available() >= prefixLengthAfterMagic) {
            // Note that we intentionally include the magic byte.
            return new SchemaAndValue<>(parse(stream.readNBytes(prefixLengthAfterMagic)), null, stream);
        }
        stream.reset();
        return null;
    }

}

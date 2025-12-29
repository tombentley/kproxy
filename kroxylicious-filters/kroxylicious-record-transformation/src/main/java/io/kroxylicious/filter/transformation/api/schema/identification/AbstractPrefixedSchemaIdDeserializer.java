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

public abstract class AbstractPrefixedSchemaIdDeserializer
        implements Deserializer<ByteWireId> {

    private final int magic;
    private final int prefixLengthWithMagic;

    AbstractPrefixedSchemaIdDeserializer(int magic, int prefixLengthWithMagic) {
        this.magic = magic;
        this.prefixLengthWithMagic = prefixLengthWithMagic;
    }

    @Override
    public ByteWireId deserialize(InputStream in, Context context) throws IOException {
        in.mark(1);
        int maybeMagic = in.read();
        in.reset();
        if (maybeMagic == magic && in.available() >= prefixLengthWithMagic) {
            // Note that we intentionally include the magic byte.
            return new ByteWireId(in.readNBytes(prefixLengthWithMagic));
        }
        return null;
    }

    @Override
    public Class<ByteWireId> returnedType() {
        return ByteWireId.class;
    }
}

/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.transformation.api.schema.identification;

import java.io.IOException;
import java.util.List;

import org.apache.kafka.common.header.Header;

import io.kroxylicious.filter.transformation.RecordDataLocation;
import io.kroxylicious.filter.transformation.TransformationInputStream;

public abstract class PrefixedDataIdentificationStrategy
        implements InputSchemaIdentification<ByteWireId>, OutputSchemaIdentification<ByteWireId> {

    private final int magic;
    private final int prefixLengthWithMagic;

    PrefixedDataIdentificationStrategy(int magic, int prefixLengthWithMagic) {
        this.magic = magic;
        this.prefixLengthWithMagic = prefixLengthWithMagic;
    }

    @Override
    public ByteWireId schemaIdFromData(List<Header> headers, RecordDataLocation site, TransformationInputStream in) throws IOException {
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
    public byte[] prefix(ByteWireId schemaId) {
        if (schemaId.bytes().length == prefixLengthWithMagic && schemaId.bytes()[0] == magic) {
            return schemaId.bytes();
        }
        else {
            throw new RuntimeException(String.format("Unexpected prefix of %s bytes", schemaId.bytes().length));
        }

    }

    @Override
    public List<Header> headers(ByteWireId schemaId, RecordDataLocation site) {
        return List.of();
    }

    @Override
    public Class<ByteWireId> returnedType() {
        return ByteWireId.class;
    }

    @Override
    public Class<ByteWireId> acceptedType() {
        return ByteWireId.class;
    }
}

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

public abstract class PrefixedDataIdentificationStrategy implements InputSchemaIdentification, OutputSchemaIdentification {

    private final int magic;
    private final int prefixLengthWithMagic;

    PrefixedDataIdentificationStrategy(int magic, int prefixLengthWithMagic) {
        this.magic = magic;
        this.prefixLengthWithMagic = prefixLengthWithMagic;
    }

    @Override
    public WireSchemaId schemaIdFromData(List<Header> headers, RecordDataLocation site, TransformationInputStream in) throws IOException {
        in.mark(1);
        int maybeMagic = in.read();
        in.reset();
        if (maybeMagic == magic && in.available() >= prefixLengthWithMagic) {
            // Note that we intentionally include the magic byte.
            return new ByteWireId(in.readNBytes(prefixLengthWithMagic));
        }
        return NoSchema.INSTANCE;
    }

    @Override
    public byte[] prefix(WireSchemaId schemaId) {
        if (schemaId instanceof ByteWireId prefix) {
            if (prefix.bytes().length == prefixLengthWithMagic && prefix.bytes()[0] == magic) {
                return prefix.bytes();
            }
            else {
                throw new RuntimeException(String.format("Unexpected prefix of %s bytes", prefix.bytes().length));
            }
        }
        else if (schemaId instanceof NoSchema) {
            return new byte[0];
        }
        else {
            throw new RuntimeException(String.format("Unexpected schema id type %s", schemaId.getClass().getName()));
        }
    }

    @Override
    public List<Header> headers(WireSchemaId schemaId, RecordDataLocation site) {
        return List.of();
    }
}

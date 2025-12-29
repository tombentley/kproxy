/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.transformation.api.schema.identification;

import java.util.List;

import org.apache.kafka.common.header.Header;

import io.kroxylicious.filter.transformation.RecordDataLocation;

public abstract class AbstractPrefixedSerializer
        implements OutputSchemaIdentification<ByteWireId> {

    private final int magic;
    private final int prefixLengthWithMagic;

    AbstractPrefixedSerializer(int magic, int prefixLengthWithMagic) {
        this.magic = magic;
        this.prefixLengthWithMagic = prefixLengthWithMagic;
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
    public Class<ByteWireId> acceptedType() {
        return ByteWireId.class;
    }
}

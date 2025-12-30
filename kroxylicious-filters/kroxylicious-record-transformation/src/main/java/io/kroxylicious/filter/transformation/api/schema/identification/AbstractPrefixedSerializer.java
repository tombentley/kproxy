/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.transformation.api.schema.identification;

import java.io.IOException;
import java.io.OutputStream;
import java.util.List;

import org.apache.kafka.common.header.Header;

import io.kroxylicious.filter.transformation.RecordDataLocation;
import io.kroxylicious.filter.transformation.api.Type;
import io.kroxylicious.filter.transformation.api.TypeException;

public abstract class AbstractPrefixedSerializer
        implements OutputSchemaIdentification<ByteWireId> {

    private final int magic;
    private final int prefixLengthWithMagic;

    AbstractPrefixedSerializer(int magic, int prefixLengthWithMagic) {
        this.magic = magic;
        this.prefixLengthWithMagic = prefixLengthWithMagic;
    }

    @Override
    public List<Header> prefix(RecordDataLocation site, ByteWireId schemaId, OutputStream outputStream) throws IOException {
        if (schemaId.bytes().length == prefixLengthWithMagic && schemaId.bytes()[0] == magic) {
            outputStream.write(magic);
            outputStream.write(schemaId.bytes());
        }
        else {
            throw new RuntimeException(String.format("Unexpected prefix of %s bytes", schemaId.bytes().length));
        }
        return List.of();
    }

    @Override
    public Type<?, ?, ?> typeCheck(Type<?, ?, ?> type) {
        if (type.wireSchemaId() != WireSchemaId.class) {
            throw new TypeException("Not a WireSchemaId: " + type);
        }
        return null;
    }

}

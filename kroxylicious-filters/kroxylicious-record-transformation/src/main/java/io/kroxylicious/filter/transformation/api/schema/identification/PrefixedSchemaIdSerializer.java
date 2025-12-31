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

import io.kroxylicious.filter.transformation.api.RecordDataLocation;
import io.kroxylicious.filter.transformation.api.Type;
import io.kroxylicious.filter.transformation.api.TypeException;

public abstract class PrefixedSchemaIdSerializer<W extends WireSchemaId>
        implements SchemaIdSerializer<W> {

    private final byte magic;

    PrefixedSchemaIdSerializer(byte magic) {
        this.magic = magic;
    }

    @Override
    public List<Header> serializeSchemaId(RecordDataLocation site, W schemaId, OutputStream outputStream) throws IOException {
        byte[] bytes = toBytes(schemaId);
        outputStream.write(magic);
        outputStream.write(bytes);

        return List.of();
    }

    protected abstract byte[] toBytes(W schemaId);

    @Override
    public Type<?, ?, ?> typeCheck(Type<?, ?, ?> type) {
        if (type.wireSchemaId() != FourByteId.class
                && type.wireSchemaId() != EightByteId.class) {
            throw new TypeException("Not a FourByteId or a EightByteId: " + type);
        }
        return type;
    }

}

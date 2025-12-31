/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.transformation.api.schema.identification;

public class FourByteSchemaIdSerializer<W extends WireSchemaId> extends PrefixedSchemaIdSerializer<W> {
    FourByteSchemaIdSerializer() {
        super((byte) 0x00);
    }

    @Override
    protected byte[] toBytes(W schemaId) {
        byte[] bytes;
        if (schemaId instanceof FourByteId fourByteId) {
            bytes = fourByteId.toBytes();
        }
        else if (schemaId instanceof EightByteId eightByteId) {
            bytes = new FourByteId(Math.toIntExact(eightByteId.id())).toBytes();
        }
        else {
            throw new RuntimeException(String.format("Unexpected type of schema id %s", schemaId));
        }
        return bytes;
    }
}

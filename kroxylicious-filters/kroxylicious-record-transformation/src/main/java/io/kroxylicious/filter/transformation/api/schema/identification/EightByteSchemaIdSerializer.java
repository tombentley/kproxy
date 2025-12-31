/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.transformation.api.schema.identification;

public class EightByteSchemaIdSerializer<W extends WireSchemaId> extends PrefixedSchemaIdSerializer<W> {
    EightByteSchemaIdSerializer() {
        super((byte) 0x00);
    }

    protected byte[] toBytes(W schemaId) {
        byte[] bytes;
        if (schemaId instanceof FourByteId fourByteId) {
            bytes = new EightByteId(fourByteId.id()).toBytes();
        }
        else if (schemaId instanceof EightByteId eightByteId) {
            bytes = eightByteId.toBytes();
        }
        else {
            throw new RuntimeException(String.format("Unexpected type of schema id %s", schemaId));
        }
        return bytes;
    }
}

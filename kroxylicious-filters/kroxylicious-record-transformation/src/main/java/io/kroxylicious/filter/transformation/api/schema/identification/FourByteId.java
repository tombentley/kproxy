/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.transformation.api.schema.identification;

public record FourByteId(int id) implements WireSchemaId {

    public byte[] toBytes() {
        return new byte[] {
                (byte) ((id & 0xFF000000) >> 24),
                (byte) ((id & 0x00FF0000) >> 16),
                (byte) ((id & 0x0000FF00) >> 8),
                (byte) ((id & 0x000000FF))
        };
    }

    public static FourByteId fromBytes(byte[] bytes) {
        return new FourByteId(
                (bytes[0]) << 24
                | (bytes[1]) << 16
                | (bytes[2]) << 8
                | (bytes[3]));
    }
}

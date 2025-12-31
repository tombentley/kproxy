/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.transformation.api.schema.identification;

public record EightByteId(long id) implements WireSchemaId {

    public byte[] toBytes() {
        return new byte[] {
            (byte) ((id & 0xFF00000000000000L) >> 56),
            (byte) ((id & 0x00FF000000000000L) >> 48),
            (byte) ((id & 0x0000FF0000000000L) >> 40),
            (byte) ((id & 0x000000FF00000000L) >> 32),
            (byte) ((id & 0x00000000FF000000L) >> 24),
            (byte) ((id & 0x0000000000FF0000L) >> 16),
            (byte) ((id & 0x000000000000FF00L) >> 8),
            (byte) ((id & 0x00000000000000FFL))
        };
    }

    public static EightByteId fromBytes(byte[] bytes) {
        return new EightByteId(
                ((long) bytes[0]) << 56
                        | ((long) bytes[1]) << 48
                        | ((long) bytes[2]) << 40
                        | ((long) bytes[3]) << 32
                        | ((long) bytes[4]) << 24
                        | ((long) bytes[5]) << 16
                        | ((long) bytes[6]) << 8
                        | ((long) bytes[7]));
    }
}

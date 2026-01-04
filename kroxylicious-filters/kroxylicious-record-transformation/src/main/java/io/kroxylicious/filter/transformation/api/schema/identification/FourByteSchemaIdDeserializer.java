/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.transformation.api.schema.identification;

/**
 * The schema identification strategy used by Confluent Schema Registry: a 5 byte prefix to the data.
 * The first byte is the zero ('magic') byte, followed by a 4 byte identifier.
 */
public class FourByteSchemaIdDeserializer extends PrefixedSchemaIdDeserializer<FourByteId> {

    public static final FourByteSchemaIdDeserializer INSTANCE = new FourByteSchemaIdDeserializer();

    FourByteSchemaIdDeserializer() {
        super((byte) 0x00, 4);
    }

    @Override
    Class<FourByteId> wireSchemaIdClass() {
        return FourByteId.class;
    }

    @Override
    FourByteId parse(byte[] bytes) {
        return FourByteId.fromBytes(bytes);
    }
}


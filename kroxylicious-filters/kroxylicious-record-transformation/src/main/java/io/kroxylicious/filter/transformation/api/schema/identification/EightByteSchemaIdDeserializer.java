/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.transformation.api.schema.identification;

/**
 * The schema identification strategy used by Apicurio Schema Registry: a 9 byte prefix to the data.
 * The first byte is the zero ('magic') byte, followed by an 8 byte identifier.
 */
public class EightByteSchemaIdDeserializer extends PrefixedSchemaIdDeserializer<EightByteId> {

    EightByteSchemaIdDeserializer() {
        super((byte) 0x00, 8);
    }

    @Override
    Class<EightByteId> wireSchemaIdClass() {
        return EightByteId.class;
    }

    @Override
    EightByteId parse(byte[] bytes) {
        return EightByteId.fromBytes(bytes);
    }
}

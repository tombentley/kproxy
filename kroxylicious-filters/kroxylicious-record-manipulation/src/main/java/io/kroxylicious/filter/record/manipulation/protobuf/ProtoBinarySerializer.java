/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.record.manipulation.protobuf;

import java.nio.ByteBuffer;
import java.util.function.Function;

import com.google.protobuf.DynamicMessage;

/**
 * Serializes a {@link DynamicMessage} to a {@link ByteBuffer} ready to be read - the inverse of
 * {@link ProtoBinaryDeserializer}. Mirrors {@link io.kroxylicious.filter.record.manipulation.avro.AvroBinarySerializer},
 * but needs no schema of its own: unlike Avro's {@code GenericRecord}, a {@link DynamicMessage} already
 * carries its {@link com.google.protobuf.Descriptors.Descriptor} internally, so there's nothing external
 * to pass in.
 */
public class ProtoBinarySerializer implements Function<DynamicMessage, ByteBuffer> {

    /**
     * Creates a serializer.
     */
    public ProtoBinarySerializer() {
    }

    @Override
    public ByteBuffer apply(DynamicMessage message) {
        return ByteBuffer.wrap(message.toByteArray());
    }
}

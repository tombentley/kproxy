/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.record.manipulation.format.protobuf;

import java.nio.ByteBuffer;
import java.util.function.Function;

import com.google.protobuf.DynamicMessage;

import io.kroxylicious.filter.record.manipulation.format.SerializationException;
import io.kroxylicious.filter.record.manipulation.format.Serializer;
import io.kroxylicious.filter.record.manipulation.format.avro.AvroBinarySerializer;

/**
 * Serializes a {@link DynamicMessage} to a {@link ByteBuffer} ready to be read - the inverse of
 * {@link ProtobufBinaryDeserializer}. Mirrors {@link AvroBinarySerializer},
 * but needs no schema of its own: unlike Avro's {@code GenericRecord}, a {@link DynamicMessage} already
 * carries its {@link com.google.protobuf.Descriptors.Descriptor} internally, so there's nothing external
 * to pass in.
 */
public class ProtobufBinarySerializer implements Function<DynamicMessage, ByteBuffer>, Serializer<DynamicMessage> {

    /**
     * Creates a serializer.
     */
    public ProtobufBinarySerializer() {
    }

    @Override
    public ByteBuffer apply(DynamicMessage message) {
        return serialize(message);
    }

    @Override
    public ByteBuffer serialize(DynamicMessage message) {
        try {
            return ByteBuffer.wrap(message.toByteArray());
        }
        catch (Exception e) {
            throw new SerializationException(e);
        }
    }
}

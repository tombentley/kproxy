/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.record.manipulation.protobuf;

import java.nio.ByteBuffer;
import java.util.function.Function;

import com.google.protobuf.CodedInputStream;
import com.google.protobuf.Descriptors;
import com.google.protobuf.DynamicMessage;

import io.kroxylicious.filter.record.manipulation.format.DeserializationException;
import io.kroxylicious.filter.record.manipulation.format.Deserializer;

/**
 * Deserializes the remaining bytes of a {@link ByteBuffer} to a {@link DynamicMessage}, decoded against a
 * fixed {@link Descriptors.Descriptor} (no schema evolution: the same schema is used to write and read).
 * Mirrors {@link io.kroxylicious.filter.record.manipulation.avro.AvroBinaryDeserializer} - unlike Avro's
 * binary encoding, Protobuf's wire format doesn't need a special-cased array-backed path, since
 * {@link CodedInputStream#newInstance(ByteBuffer)} already reads directly from the buffer without copying.
 */
public class ProtobufBinaryDeserializer implements Function<ByteBuffer, DynamicMessage>, Deserializer<DynamicMessage> {

    private final Descriptors.Descriptor descriptor;

    /**
     * Creates a deserializer.
     * @param descriptor the schema the input conforms to
     */
    public ProtobufBinaryDeserializer(Descriptors.Descriptor descriptor) {
        this.descriptor = descriptor;
    }

    @Override
    public DynamicMessage apply(ByteBuffer bb) {
        return deserialize(bb);
    }

    @Override
    public DynamicMessage deserialize(ByteBuffer bb) {
        try {
            return DynamicMessage.parseFrom(descriptor, CodedInputStream.newInstance(bb));
        }
        catch (Exception e) {
            throw new DeserializationException(e);
        }
    }
}

/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.record.manipulation.protobuf;

import com.google.protobuf.Descriptors;
import com.google.protobuf.DynamicMessage;

import io.kroxylicious.filter.record.manipulation.format.DataFormat;
import io.kroxylicious.filter.record.manipulation.format.Deserializer;
import io.kroxylicious.filter.record.manipulation.format.Serializer;

public class ProtobufBinaryFormat implements DataFormat<DynamicMessage> {

    private final Descriptors.Descriptor descriptor;

    public ProtobufBinaryFormat(Descriptors.Descriptor descriptor) {
        this.descriptor = descriptor;
    }

    @Override
    public Serializer<DynamicMessage> serializer() {
        return new ProtobufBinarySerializer();
    }

    @Override
    public Deserializer<DynamicMessage> deserializer() {
        return new ProtobufBinaryDeserializer(descriptor);
    }

}

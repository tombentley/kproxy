/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.transform.neo.format.protobuf;

import java.lang.reflect.Type;
import java.nio.ByteBuffer;
import java.util.Map;

import io.kroxylicious.proxy.plugin.Plugin;
import io.kroxylicious.transform.neo.format.avro.WriteAvro;
import io.kroxylicious.transform.neo.op.BaseTypedOp;
import io.kroxylicious.transform.neo.op.OpFactory;
import io.kroxylicious.transform.service.TransformInitializationContext;

/**
 * Serializes a {@link ProtoValue} to Protobuf's binary encoding - the Protobuf equivalent of
 * {@link WriteAvro}, and the inverse of
 * {@link DeserializeProtobuf}. Takes no withKeySchema config: unlike Avro's {@code GenericRecord}, a
 * {@link com.google.protobuf.DynamicMessage} already carries its own
 * {@link com.google.protobuf.Descriptors.Descriptor}, so there's nothing to do beyond unwrapping
 * {@link ProtoValue#message()}.
 */
@Plugin(configType = Void.class)
public class SerializeProtobuf implements OpFactory<ProtoValue, ByteBuffer> {

    @Override
    public BaseTypedOp<ProtoValue, ByteBuffer> create(Map<String, Object> config,
                                                      TransformInitializationContext lookup,
                                                      Type argumentType) {
        var serializer = new ProtobufBinarySerializer();
        return BaseTypedOp.of(ProtoValue.class, ByteBuffer.class, (value, opContext) -> serializer.serialize(value.message()));
    }
}

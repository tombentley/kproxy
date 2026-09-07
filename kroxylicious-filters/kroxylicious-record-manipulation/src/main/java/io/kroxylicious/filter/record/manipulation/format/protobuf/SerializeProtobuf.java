/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.record.manipulation.format.protobuf;

import java.lang.reflect.Type;
import java.nio.ByteBuffer;
import java.util.Map;

import io.kroxylicious.filter.record.manipulation.common.PluginLookup;
import io.kroxylicious.filter.record.manipulation.common.StaticTypedOp;
import io.kroxylicious.filter.record.manipulation.op.BaseTypedOp;
import io.kroxylicious.filter.record.manipulation.op.OpContext;
import io.kroxylicious.filter.record.manipulation.op.OpFactory;
import io.kroxylicious.proxy.plugin.Plugin;

/**
 * Serializes a {@link ProtoValue} to Protobuf's binary encoding - the Protobuf equivalent of
 * {@link io.kroxylicious.filter.record.manipulation.format.avro.SerializeAvro}, and the inverse of
 * {@link DeserializeProtobuf}. Takes no schema config: unlike Avro's {@code GenericRecord}, a
 * {@link com.google.protobuf.DynamicMessage} already carries its own
 * {@link com.google.protobuf.Descriptors.Descriptor}, so there's nothing to do beyond unwrapping
 * {@link ProtoValue#message()}.
 */
@Plugin(configType = Void.class)
public class SerializeProtobuf implements OpFactory<ProtoValue, ByteBuffer> {

    @Override
    public BaseTypedOp<ProtoValue, ByteBuffer> create(Map<String, Object> config, PluginLookup lookup, Type argumentType) {
        var serializer = new ProtobufBinarySerializer();
        return new StaticTypedOp<ProtoValue, ByteBuffer>() {
            @Override
            public Type outputType(Type inputType) {
                return ByteBuffer.class;
            }

            @Override
            public ByteBuffer apply(ProtoValue value, OpContext opContext) {
                return serializer.serialize(value.message());
            }
        };
    }
}

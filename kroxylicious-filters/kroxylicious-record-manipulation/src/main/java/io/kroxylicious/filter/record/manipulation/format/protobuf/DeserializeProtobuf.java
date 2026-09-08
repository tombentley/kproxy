/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.record.manipulation.format.protobuf;

import java.lang.reflect.Type;
import java.nio.ByteBuffer;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.kroxylicious.filter.record.manipulation.common.PluginLookup;
import io.kroxylicious.filter.record.manipulation.op.BaseTypedOp;
import io.kroxylicious.filter.record.manipulation.op.OpFactory;
import io.kroxylicious.proxy.plugin.Plugin;

/**
 * Deserializes a record value/key from Protobuf's binary encoding to a {@link ProtoValue} - the Protobuf
 * equivalent of {@link io.kroxylicious.filter.record.manipulation.format.avro.DeserializeAvro}.
 * <p>
 * The only op in the Protobuf pipeline that parses schema text - see {@link ProtoValue} for why
 * {@link ProtobufTransform}/{@link SerializeProtobuf} instead consume whatever schema arrives bundled
 * with the message, rather than parsing their own.
 */
@Plugin(configType = ProtoSchema.class)
public class DeserializeProtobuf implements OpFactory<ByteBuffer, ProtoValue> {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Override
    public BaseTypedOp<ByteBuffer, ProtoValue> create(Map<String, Object> config, PluginLookup lookup, Type argumentType) {
        ProtoSchema c = MAPPER.convertValue(config, ProtoSchema.class);
        ParsedProtoSchema schema = ProtobufSchemaParser.parse(c.protoText(), c.rootMessageName());
        var deserializer = new ProtobufBinaryDeserializer(schema.descriptor());
        return BaseTypedOp.of(ByteBuffer.class, ProtoValue.class, (value, opContext) -> new ProtoValue(deserializer.deserialize(value), schema));
    }
}

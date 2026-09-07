/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.record.manipulation.protobuf;

import com.google.protobuf.DynamicMessage;

import io.kroxylicious.filter.record.manipulation.op.BaseTypedOp;
import io.kroxylicious.filter.record.manipulation.common.PluginLookup;
import io.kroxylicious.filter.record.manipulation.format.DataFormat;
import io.kroxylicious.filter.record.manipulation.format.DataFormatService;
import io.kroxylicious.filter.record.manipulation.format.SchemaParseException;

public class ProtobufBinaryFormatService implements DataFormatService<DynamicMessage, ProtoSchema, ProtoSchema> {
    @Override
    public DataFormat<DynamicMessage> create(ProtoSchema schemaConfiguration) {
        try {
            ParsedProtoSchema parse = ProtobufSchemaParser.parse(schemaConfiguration.protoText(), schemaConfiguration.rootMessageName());
            return new ProtobufBinaryFormat(parse.descriptor());
        }
        catch (Exception e) {
            throw new SchemaParseException(e);
        }
    }

    @Override
    public BaseTypedOp<DynamicMessage, DynamicMessage> operator(ProtoSchema operatorConfiguration) {
        ParsedProtoSchema maskSchema = ProtobufSchemaParser.parse(operatorConfiguration.protoText(), operatorConfiguration.rootMessageName());
        PluginLookup lookup = null;
        return (BaseTypedOp) ProtobufFunction.buildMask(maskSchema, lookup);
    }
}

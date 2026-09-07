/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.record.manipulation.avro;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import org.apache.avro.Schema;
import org.apache.avro.generic.GenericRecord;

import io.kroxylicious.filter.record.manipulation.op.BaseTypedOp;
import io.kroxylicious.filter.record.manipulation.format.DataFormat;
import io.kroxylicious.filter.record.manipulation.format.DataFormatService;
import io.kroxylicious.filter.record.manipulation.format.SchemaParseException;

public class AvroBinaryFormatService implements DataFormatService<GenericRecord, byte[], Schema> {

    @Override
    public DataFormat<GenericRecord> create(byte[] schemaConfiguration) {
        try (var in = new ByteArrayInputStream(schemaConfiguration)) {
            return new AvroBinaryFormat(new Schema.Parser().parse(in));
        }
        catch (IOException e) {
            throw new SchemaParseException(e);
        }
    }

    @Override
    public BaseTypedOp<GenericRecord, GenericRecord> operator(Schema operatorConfiguration) {
        return (BaseTypedOp) AvroFunction.buildMask(operatorConfiguration, null, null);
    }


}

/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.transformation.format.avro;

import java.io.IOException;
import java.io.InputStream;

import org.apache.avro.Schema;
import org.apache.avro.generic.GenericDatumReader;
import org.apache.avro.io.BinaryDecoder;
import org.apache.avro.io.Decoder;
import org.apache.avro.io.DecoderFactory;

import io.kroxylicious.filter.transformation.api.format.Deserializer;
import io.kroxylicious.filter.transformation.api.mapper.Context;

/**
 * A deserializer for Avro-serialized data.
 */
public class AvroDeserializer implements Deserializer<AvroValue> {

    private final boolean binary;
    private final Schema schema;
    private final GenericDatumReader<Object> reader;
    private BinaryDecoder decoder;

    public AvroDeserializer(Schema schema, boolean binary) {
        this.schema = schema;
        this.binary = binary;
        this.reader = new GenericDatumReader<>(schema);
    }

    @Override
    public Class<AvroValue> returnedType() {
        return AvroValue.class;
    }

    @Override
    public AvroValue deserialize(InputStream in, Context context) throws IOException {
        DecoderFactory decoderFactory = DecoderFactory.get();
        Decoder decoder;
        if (binary) {
            decoder = this.decoder = decoderFactory.binaryDecoder(in, this.decoder);
        }
        else {
            decoder = decoderFactory.jsonDecoder(schema, in);
        }

        Object read = reader.read(null, decoder);

        return new AvroValue(reader.getSchema(), read);
    }
}

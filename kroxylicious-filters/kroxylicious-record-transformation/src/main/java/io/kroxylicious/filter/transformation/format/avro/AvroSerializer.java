/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.transformation.format.avro;

import java.io.IOException;
import java.io.OutputStream;

import org.apache.avro.Schema;
import org.apache.avro.generic.GenericDatumWriter;
import org.apache.avro.io.BinaryEncoder;
import org.apache.avro.io.Encoder;
import org.apache.avro.io.EncoderFactory;

import io.kroxylicious.filter.transformation.api.format.Serializer;

public class AvroSerializer implements Serializer<AvroValue> {

    private BinaryEncoder encoder;

    private boolean binary;

    @Override
    public Class<AvroValue> acceptedType() {
        return AvroValue.class;
    }

    @Override
    public void serialize(AvroValue value, OutputStream out) throws IOException {

        Schema schema = value.schema();

        EncoderFactory encoderFactory = EncoderFactory.get();
        Encoder encoder;
        if (binary) {
            encoder = this.encoder = encoderFactory.directBinaryEncoder(out, this.encoder);
        }
        else {
            encoder = encoderFactory.jsonEncoder(schema, out);
        }

        var writer = new GenericDatumWriter<>(schema);
        writer.write(value.value(), encoder);
        encoder.flush();
    }
}

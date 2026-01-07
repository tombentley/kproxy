/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.transformation.aafresh.avro;

import java.io.IOException;
import java.io.OutputStream;

import org.apache.avro.Schema;
import org.apache.avro.generic.GenericDatumWriter;
import org.apache.avro.io.BinaryEncoder;
import org.apache.avro.io.Encoder;
import org.apache.avro.io.EncoderFactory;

import io.kroxylicious.filter.transformation.aafresh.Ser;

public class AvroSer implements Ser<AvroRoot> {

    private final GenericDatumWriter<Object> writer;
    private BinaryEncoder cachedEncoder;

    public AvroSer(Schema schema) {
        this.writer = new GenericDatumWriter<>(schema);
    }

    @Override
    public void serialize(AvroRoot value, OutputStream out) throws IOException {

        EncoderFactory encoderFactory = EncoderFactory.get();
        Encoder encoder = this.cachedEncoder = encoderFactory.directBinaryEncoder(out, this.cachedEncoder);

        writer.write(value.root(), encoder);
        encoder.flush();
    }
}

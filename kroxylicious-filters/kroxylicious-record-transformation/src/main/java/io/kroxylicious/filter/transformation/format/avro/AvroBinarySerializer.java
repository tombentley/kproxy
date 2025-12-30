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

import io.kroxylicious.filter.transformation.api.TypeException;
import io.kroxylicious.filter.transformation.api.Type;
import io.kroxylicious.filter.transformation.api.format.Serializer;

public class AvroBinarySerializer implements Serializer<Object> {

    private final GenericDatumWriter<Object> writer;
    private BinaryEncoder encoder;

    private boolean binary;

    private Schema schema;

    public AvroBinarySerializer(Schema schema, boolean binary) {
        this.schema = schema;
        this.binary = binary;
        this.writer = new GenericDatumWriter<>(schema);
    }

    @Override
    public void accepts(Type<?, ?, ?> type) {
        if (type.schema() != Schema.class) {
            throw new TypeException("");
        }
    }

    @Override
    public void serialize(Object value, OutputStream out) throws IOException {

        EncoderFactory encoderFactory = EncoderFactory.get();
        Encoder encoder;
        if (binary) {
            encoder = this.encoder = encoderFactory.directBinaryEncoder(out, this.encoder);
        }
        else {
            encoder = encoderFactory.jsonEncoder(schema, out);
        }

        writer.write(value, encoder);
        encoder.flush();
    }
}

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
import org.apache.avro.io.Encoder;
import org.apache.avro.io.EncoderFactory;

import io.kroxylicious.filter.transformation.api.TypeException;
import io.kroxylicious.filter.transformation.api.Type;
import io.kroxylicious.filter.transformation.api.format.Serializer;

public class AvroJsonSerializer implements Serializer<Object> {

    private final Schema schema;
    private final GenericDatumWriter<Object> writer;

    public AvroJsonSerializer(Schema schema) {
        this.schema = schema;
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
        Encoder encoder = encoderFactory.jsonEncoder(schema, out);
        writer.write(value, encoder);
        encoder.flush();
    }
}

/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.transformation.format.avro;

import java.io.IOException;
import java.io.OutputStream;

import io.kroxylicious.filter.transformation.api.format.Serializer;

public class AvroSerializer implements Serializer<AvroValue> {
    @Override
    public Class<AvroValue> acceptedType() {
        return AvroValue.class;
    }

    @Override
    public void serialize(AvroValue value, OutputStream out) throws IOException {
        /*
        Schema schema = value.schema();
        EncoderFactory encoderFactory = EncoderFactory.get();
        var encoder = encoderFactory.directBinaryEncoder(out, null);

        var writer = new GenericDatumWriter<>(schema);
        writer.write(value.value(), encoder);
        encoder.flush();
         */

    }
}

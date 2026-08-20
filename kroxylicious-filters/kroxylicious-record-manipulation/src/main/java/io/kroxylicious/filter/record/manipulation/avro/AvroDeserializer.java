/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.record.manipulation.avro;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.util.function.Function;

import org.apache.avro.generic.GenericData;
import org.apache.avro.generic.GenericDatumReader;

public class AvroDeserializer implements Function<ByteBuffer, GenericData> {
    @Override
    public GenericData apply(ByteBuffer byteBuffer) {
        GenericDatumReader<GenericData> datumReader = new GenericDatumReader<>();
        datumReader.setSchema(null);
        try {
            return datumReader.read(null, null);
        }
        catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}

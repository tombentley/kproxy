/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.transformation.aafresh.avro;

import java.io.IOException;
import java.io.InputStream;

import org.apache.avro.Schema;
import org.apache.avro.generic.GenericDatumReader;
import org.apache.avro.io.BinaryDecoder;
import org.apache.avro.io.Decoder;
import org.apache.avro.io.DecoderFactory;

import io.kroxylicious.filter.transformation.aafresh.Deser;

public class AvroDerser implements Deser<AvroRoot> {

    private final GenericDatumReader<Object> reader;
    private BinaryDecoder cachedDecoder;

    public AvroDerser(Schema schema) {
        this.reader = new GenericDatumReader<>(schema);
    }

    @Override
    public AvroRoot deser(InputStream in) throws IOException {
        DecoderFactory decoderFactory = DecoderFactory.get();
        Decoder decoder = this.cachedDecoder = decoderFactory.binaryDecoder(in, this.cachedDecoder);
        Object read = reader.read(null, decoder);
        return new AvroRoot(read, reader.getSchema());
    }
}

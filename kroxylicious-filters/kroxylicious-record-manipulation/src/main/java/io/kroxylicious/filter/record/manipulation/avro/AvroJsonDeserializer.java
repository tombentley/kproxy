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

import org.apache.avro.Schema;
import org.apache.avro.generic.GenericDatumReader;
import org.apache.avro.generic.GenericRecord;
import org.apache.avro.io.Decoder;
import org.apache.avro.io.DecoderFactory;
import org.apache.kafka.common.utils.ByteBufferInputStream;

import io.kroxylicious.filter.record.manipulation.format.Deserializer;

/**
 * Deserializes the remaining bytes of a {@link ByteBuffer} to a {@link GenericRecord}, decoded per Avro's
 * JSON encoding (not plain/canonical JSON - Avro's own schema-directed JSON codec) against a fixed
 * {@link Schema}. Mirrors {@link AvroBinaryDeserializer}, but via
 * {@link org.apache.avro.io.JsonDecoder} rather than {@link org.apache.avro.io.BinaryDecoder} - Avro's
 * JSON codec only decodes from a stream, so there's no array-backed fast path to mirror here.
 */
public class AvroJsonDeserializer implements Function<ByteBuffer, GenericRecord>, Deserializer<GenericRecord> {

    private final Schema schema;
    private final GenericDatumReader<GenericRecord> reader;

    /**
     * Creates a deserializer.
     * @param schema the schema the input conforms to
     */
    public AvroJsonDeserializer(Schema schema) {
        this.schema = schema;
        this.reader = new GenericDatumReader<>(schema);
    }

    @Override
    public GenericRecord apply(ByteBuffer bb) {
        return deserialize(bb);
    }

    @Override
    public GenericRecord deserialize(ByteBuffer bb) {
        try (var is = new ByteBufferInputStream(bb)) {
            Decoder decoder = DecoderFactory.get().jsonDecoder(schema, is);
            return reader.read(null, decoder);
        }
        catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}

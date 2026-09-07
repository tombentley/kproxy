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
import org.apache.avro.generic.GenericDatumWriter;
import org.apache.avro.generic.GenericRecord;
import org.apache.avro.io.Encoder;
import org.apache.avro.io.EncoderFactory;
import org.apache.kafka.common.utils.ByteBufferOutputStream;

import io.kroxylicious.filter.record.manipulation.format.Serializer;

/**
 * Serializes a {@link GenericRecord} to a {@link ByteBuffer} ready to be read, using Avro's JSON encoding -
 * the inverse of {@link AvroJsonDeserializer}. Mirrors {@link AvroBinarySerializer}, but via
 * {@link org.apache.avro.io.JsonEncoder} rather than {@link org.apache.avro.io.BinaryEncoder}.
 */
public class AvroJsonSerializer implements Function<GenericRecord, ByteBuffer>, Serializer<GenericRecord> {

    private final Schema schema;
    private final GenericDatumWriter<GenericRecord> writer;

    /**
     * Creates a serializer.
     * @param schema the schema written records conform to
     */
    public AvroJsonSerializer(Schema schema) {
        this.schema = schema;
        this.writer = new GenericDatumWriter<>(schema);
    }

    @Override
    public ByteBuffer apply(GenericRecord record) {
        return serialize(record);
    }

    @Override
    public ByteBuffer serialize(GenericRecord record) {
        // TODO buffer recycling
        try (var os = new ByteBufferOutputStream(10000)) {
            Encoder encoder = EncoderFactory.get().jsonEncoder(schema, os);
            writer.write(record, encoder);
            encoder.flush();
            ByteBuffer buffer = os.buffer();
            buffer.flip();
            return buffer;
        }
        catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}

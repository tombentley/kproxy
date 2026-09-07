/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.record.manipulation.format.avro;

import java.nio.ByteBuffer;
import java.util.function.Function;

import org.apache.avro.Schema;
import org.apache.avro.generic.GenericDatumWriter;
import org.apache.avro.generic.GenericRecord;
import org.apache.avro.io.Encoder;
import org.apache.avro.io.EncoderFactory;
import org.apache.kafka.common.utils.ByteBufferOutputStream;

import io.kroxylicious.filter.record.manipulation.format.SerializationException;
import io.kroxylicious.filter.record.manipulation.format.Serializer;
import io.kroxylicious.filter.record.manipulation.format.jackson.JacksonSerializer;

/**
 * Serializes a {@link GenericRecord} to a {@link ByteBuffer} ready to be read, using Avro's single-object
 * binary encoding - the inverse of {@link AvroBinaryDeserializer}. Mirrors
 * {@link JacksonSerializer}.
 */
public class AvroBinarySerializer implements Function<GenericRecord, ByteBuffer>, Serializer<GenericRecord> {

    private final GenericDatumWriter<GenericRecord> writer;

    /**
     * Creates a serializer.
     * @param schema the schema written records conform to
     */
    public AvroBinarySerializer(Schema schema) {
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
            Encoder encoder = EncoderFactory.get().binaryEncoder(os, null);
            writer.write(record, encoder);
            encoder.flush();
            ByteBuffer buffer = os.buffer();
            buffer.flip();
            return buffer;
        }
        catch (Exception e) {
            throw new SerializationException(e);
        }
    }
}

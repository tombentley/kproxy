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

import io.kroxylicious.filter.record.manipulation.format.SerializationException;
import io.kroxylicious.filter.record.manipulation.format.Serializer;
import io.kroxylicious.filter.record.manipulation.format.jackson2.JacksonSerializer;
import io.kroxylicious.kafka.common.utils.ByteBufferOutputStream;

/**
 * Serializes a value to a {@link ByteBuffer} ready to be read, using Avro's single-object binary encoding -
 * the inverse of {@link AvroBinaryDeserializer}. Mirrors {@link JacksonSerializer}.
 * <p>
 * Typed to {@link Object} rather than {@link GenericRecord} because {@code schema} isn't required to be a
 * {@code record} - see {@link AvroBinaryDeserializer}.
 */
public class AvroBinarySerializer implements Function<Object, ByteBuffer>, Serializer<Object> {

    private final GenericDatumWriter<Object> writer;

    /**
     * Creates a serializer.
     * @param schema the schema written values conform to
     */
    public AvroBinarySerializer(Schema schema) {
        this.writer = new GenericDatumWriter<>(schema);
    }

    @Override
    public ByteBuffer apply(Object value) {
        return serialize(value);
    }

    @Override
    public ByteBuffer serialize(Object value) {
        // TODO buffer recycling
        try (var os = new ByteBufferOutputStream(10000)) {
            Encoder encoder = EncoderFactory.get().binaryEncoder(os, null);
            writer.write(value, encoder);
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

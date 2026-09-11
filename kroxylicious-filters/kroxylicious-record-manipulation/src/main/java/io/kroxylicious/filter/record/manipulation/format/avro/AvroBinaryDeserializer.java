/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.record.manipulation.format.avro;

import java.nio.ByteBuffer;
import java.util.function.Function;

import org.apache.avro.Schema;
import org.apache.avro.generic.GenericDatumReader;
import org.apache.avro.generic.GenericRecord;
import org.apache.avro.io.Decoder;
import org.apache.avro.io.DecoderFactory;

import io.kroxylicious.filter.record.manipulation.format.DeserializationException;
import io.kroxylicious.filter.record.manipulation.format.Deserializer;
import io.kroxylicious.filter.record.manipulation.format.jackson2.JacksonDeserializer;
import io.kroxylicious.kafka.common.utils.ByteBufferInputStream;

/**
 * Deserializes the remaining bytes of a {@link ByteBuffer}, decoded per Avro's single-object binary
 * encoding against a fixed {@link Schema} (no schema evolution: the same schema is used to write and
 * read). Mirrors {@link JacksonDeserializer}, except a {@link Schema} is required up front - unlike JSON,
 * Avro binary data isn't self-describing.
 * <p>
 * Typed to {@link Object} rather than {@link GenericRecord} because {@code schema} isn't required to be a
 * {@code record} - Avro's generic codec already decodes to whatever Java type matches {@code schema}
 * (a {@link GenericRecord}, a {@link java.util.List} for an array, or a scalar/enum/fixed leaf value).
 */
public class AvroBinaryDeserializer implements Function<ByteBuffer, Object>, Deserializer<Object> {

    private final GenericDatumReader<Object> reader;

    /**
     * Creates a deserializer.
     * @param schema the schema the input conforms to
     */
    public AvroBinaryDeserializer(Schema schema) {
        this.reader = new GenericDatumReader<>(schema);
    }

    @Override
    public Object apply(ByteBuffer bb) {
        return deserialize(bb);
    }

    @Override
    public Object deserialize(ByteBuffer bb) {
        try {
            if (bb.hasArray()) {
                Decoder decoder = DecoderFactory.get().binaryDecoder(bb.array(), bb.arrayOffset() + bb.position(), bb.remaining(), null);
                return reader.read(null, decoder);
            }
            else {
                try (var is = new ByteBufferInputStream(bb)) {
                    Decoder decoder = DecoderFactory.get().binaryDecoder(is, null);
                    return reader.read(null, decoder);
                }
            }
        }
        catch (Exception e) {
            throw new DeserializationException(e);
        }
    }
}

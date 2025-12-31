/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.transformation.format.avro;

import java.io.IOException;
import java.io.InputStream;

import org.apache.avro.Schema;
import org.apache.avro.generic.GenericDatumReader;
import org.apache.avro.io.Decoder;
import org.apache.avro.io.DecoderFactory;

import io.kroxylicious.filter.transformation.TransformationInputStream;
import io.kroxylicious.filter.transformation.api.TypeException;
import io.kroxylicious.filter.transformation.api.SchemaAndValue;
import io.kroxylicious.filter.transformation.api.Type;
import io.kroxylicious.filter.transformation.api.format.Deserializer;
import io.kroxylicious.filter.transformation.api.mapper.Context;
import io.kroxylicious.filter.transformation.api.schema.identification.NoSchemaId;

/**
 * A deserializer for Avro-serialized data.
 */
public class AvroJsonDeserializer implements Deserializer<Schema, Object> {

    private final Schema schema;
    private final GenericDatumReader<Object> reader;

    @Override
    public Type<?, ?, ?> typeCheck(Type<?, ?, ?> type) {
        if (!TransformationInputStream.class.isAssignableFrom(type.cls())) {
            throw new TypeException(String.format("Type %s is not assignable to InputStream", type));
        }
        return new Type(NoSchemaId.class, Schema.class, Object.class);
    }

    public AvroJsonDeserializer(Schema schema) {
        this.schema = schema;
        this.reader = new GenericDatumReader<>(schema);
    }

    @Override
    public SchemaAndValue<NoSchemaId, Schema, Object> deserialize(InputStream in, Context context) throws IOException {
        DecoderFactory decoderFactory = DecoderFactory.get();
        Decoder decoder = decoderFactory.jsonDecoder(schema, in);

        Object read = reader.read(null, decoder);

        return new SchemaAndValue<>(NoSchemaId.INSTANCE, reader.getSchema(), read);
    }
}

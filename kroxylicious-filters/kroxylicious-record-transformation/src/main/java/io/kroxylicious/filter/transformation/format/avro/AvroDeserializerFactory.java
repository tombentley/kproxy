/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.transformation.format.avro;

import org.apache.avro.Schema;

import io.kroxylicious.filter.transformation.api.mapper.Context;
import io.kroxylicious.filter.transformation.api.mapper.Mapper;

/**
 * Maps from an Avro {@link Schema} to an {@link AvroDeserializer}
 */
public class AvroDeserializerFactory implements Mapper<Schema, AvroDeserializer> {

    @Override
    public Class<Schema> acceptedType() {
        return Schema.class;
    }

    @Override
    public Class<AvroDeserializer> returnedType() {
        return AvroDeserializer.class;
    }

    @Override
    public AvroDeserializer transform(Schema schema, Context context) {
        return new AvroDeserializer(schema, true);
    }
}

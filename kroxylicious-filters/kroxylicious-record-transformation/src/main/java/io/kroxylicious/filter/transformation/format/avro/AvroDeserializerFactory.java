/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.transformation.format.avro;

import org.apache.avro.Schema;

import io.kroxylicious.filter.transformation.api.Type;
import io.kroxylicious.filter.transformation.api.mapper.Context;
import io.kroxylicious.filter.transformation.api.mapper.TypeCheckable;

/**
 * Maps from an Avro {@link Schema} to an {@link AvroBinaryDeserializer}
 */
public class AvroDeserializerFactory implements TypeCheckable {

    public AvroBinaryDeserializer transform(Schema schema, Context context) {
        return new AvroBinaryDeserializer(schema);
    }

    @Override
    public Type<?, ?, ?> typeCheck(Type<?, ?, ?> type) {
        return null;
    }
}

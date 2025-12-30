/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.transformation.format.avro;

import java.util.Objects;

import org.apache.avro.Schema;

import io.kroxylicious.filter.transformation.api.format.DataFormat;
import io.kroxylicious.filter.transformation.api.format.Deserializer;
import io.kroxylicious.filter.transformation.api.format.Serializer;
import io.kroxylicious.filter.transformation.api.schema.identification.WireSchemaId;

public class AvroFormat implements DataFormat<Schema, Object> {

    private final WireSchemaId wireSchemaId;
    private final Schema schema;

    public AvroFormat(WireSchemaId wireSchemaId, Schema schema) {
        this.wireSchemaId = Objects.requireNonNull(wireSchemaId);
        this.schema = Objects.requireNonNull(schema);
    }

    @Override
    public WireSchemaId schemaId() {
        return wireSchemaId;
    }

    @Override
    public Class<Object> type() {
        return Object.class;
    }

    @Override
    public Serializer<Object> serializer() {
        return new AvroBinarySerializer(schema, true);
    }

    @Override
    public Deserializer<Schema, Object> deserializer() {
        return new AvroBinaryDeserializer(schema, true);
    }
}

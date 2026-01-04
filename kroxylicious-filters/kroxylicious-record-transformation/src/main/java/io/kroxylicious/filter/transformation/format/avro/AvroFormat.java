/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.transformation.format.avro;

import java.util.Objects;
import java.util.Set;

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
    public Set<String> encodings() {
        return Set.of("binary", "json");
    }

    @Override
    public Serializer<Object> serializer(String encoding) {
        return switch (encoding) {
            case "binary" -> new AvroBinarySerializer(schema);
            case "json" -> new AvroJsonSerializer(schema);
            default -> throw new IllegalArgumentException("Unknown encoding: " + encoding);
        };
    }

    @Override
    public Deserializer<Schema, Object> deserializer(String encoding) {
        return switch (encoding) {
            case "binary" -> new AvroBinaryDeserializer(schema);
            case "json" -> new AvroJsonDeserializer(schema);
            default -> throw new IllegalArgumentException("Unknown encoding: " + encoding);
        };
    }

    @Override
    public WireSchemaId schemaId() {
        return wireSchemaId;
    }

    @Override
    public String defaultEncoding() {
        return "binary";
    }

}

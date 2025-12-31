/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.transformation.format.avro;

import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;

import org.apache.avro.Schema;

import io.kroxylicious.filter.transformation.api.format.DataFormat;
import io.kroxylicious.filter.transformation.api.format.Deserializer;
import io.kroxylicious.filter.transformation.api.format.Serializer;
import io.kroxylicious.filter.transformation.api.schema.identification.WireSchemaId;

import edu.umd.cs.findbugs.annotations.Nullable;

public class AvroFormat implements DataFormat<AvroFormat.Encoding, Schema, Object> {

    enum Encoding {
        BINARY,
        JSON
    }

    private final WireSchemaId wireSchemaId;
    private final Schema schema;

    public AvroFormat(WireSchemaId wireSchemaId, Schema schema) {
        this.wireSchemaId = Objects.requireNonNull(wireSchemaId);
        this.schema = Objects.requireNonNull(schema);
    }

    @Override
    public Set<Encoding> encodings() {
        return EnumSet.allOf(Encoding.class);
    }

    @Override
    public WireSchemaId schemaId() {
        return wireSchemaId;
    }

    @Override
    public Serializer serializer(@Nullable Encoding encoding) {
        if (encoding == null) {
            return new AvroBinarySerializer(schema);
        }
        return switch (encoding) {
            case BINARY -> new AvroBinarySerializer(schema);
            case JSON -> new AvroJsonSerializer(schema);
        };
    }

    @Override
    public Deserializer deserializer(@Nullable Encoding encoding) {
        if (encoding == null) {
            return new AvroBinaryDeserializer(schema);
        }
        return switch (encoding) {
            case BINARY -> new AvroBinaryDeserializer(schema);
            case JSON -> new AvroJsonDeserializer(schema);
        };
    }

}

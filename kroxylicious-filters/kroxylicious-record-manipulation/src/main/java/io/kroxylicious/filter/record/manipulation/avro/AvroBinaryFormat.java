/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.record.manipulation.avro;

import org.apache.avro.Schema;
import org.apache.avro.generic.GenericRecord;

import io.kroxylicious.filter.record.manipulation.format.DataFormat;
import io.kroxylicious.filter.record.manipulation.format.Deserializer;
import io.kroxylicious.filter.record.manipulation.format.Serializer;

public class AvroBinaryFormat implements DataFormat<GenericRecord> {

    private final Schema schema;

    public AvroBinaryFormat(Schema schema) {
        this.schema = schema;
    }

    @Override
    public Serializer<GenericRecord> serializer() {
        return new AvroBinarySerializer(schema);
    }

    @Override
    public Deserializer<GenericRecord> deserializer() {
        return new AvroBinaryDeserializer(schema);
    }
}

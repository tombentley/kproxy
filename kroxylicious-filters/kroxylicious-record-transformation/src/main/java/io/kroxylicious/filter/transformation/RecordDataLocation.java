/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.transformation;

import java.nio.ByteBuffer;

import org.apache.kafka.common.record.Record;

import io.kroxylicious.filter.transformation.api.schema.identification.WireSchemaId;

import edu.umd.cs.findbugs.annotations.Nullable;

/**
 * Enumerates the places within a Kafka record where some data might be located.
 */
public sealed interface RecordDataLocation permits RecordDataLocation.KeyDataLocation, RecordDataLocation.ValueDataLocation {

    KeyDataLocation KEY = new KeyDataLocation();
    ValueDataLocation VALUE = new ValueDataLocation();

    @Nullable ByteBuffer buffer(Record record);

    SchemalessDataTransform dataTransform(RecordTransform transformation);
    SchemaIdTransform<WireSchemaId, WireSchemaId> schemaIdTransform(RecordTransform transformation);

    record KeyDataLocation() implements RecordDataLocation {

        @Override
        public @Nullable ByteBuffer buffer(Record record) {
            return record.key();
        }

        @Override
        public SchemalessDataTransform dataTransform(RecordTransform transformation) {
            return transformation.keyTransform();
        }

        @Override
        public SchemaIdTransform<WireSchemaId, WireSchemaId> schemaIdTransform(RecordTransform transformation) {
            return transformation.keySchemaIdTransform();
        }
    }

    record ValueDataLocation() implements RecordDataLocation {

        @Override
        public @Nullable ByteBuffer buffer(Record record) {
            return record.value();
        }

        @Override
        public SchemalessDataTransform dataTransform(RecordTransform transformation) {
            return transformation.valueTransform();
        }

        @Override
        public SchemaIdTransform<WireSchemaId, WireSchemaId> schemaIdTransform(RecordTransform transformation) {
            return transformation.valueSchemaIdTransform();
        }

    }
}


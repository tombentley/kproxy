/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.transformation;

import java.nio.ByteBuffer;
import java.util.List;

import org.apache.kafka.common.record.Record;

import io.kroxylicious.filter.transformation.api.format.Deserializer;
import io.kroxylicious.filter.transformation.api.format.Serializer;
import io.kroxylicious.filter.transformation.api.mapper.Mapper;
import io.kroxylicious.filter.transformation.api.schema.identification.InputSchemaIdentification;
import io.kroxylicious.filter.transformation.api.schema.identification.OutputSchemaIdentification;
import io.kroxylicious.filter.transformation.api.schema.identification.SchemaTransformation;

import edu.umd.cs.findbugs.annotations.Nullable;

/**
 * Enumerates the places within a Kafka record where some data might be located.
 */
public sealed interface RecordDataLocation permits RecordDataLocation.KeyDataLocation, RecordDataLocation.ValueDataLocation {

    @Nullable ByteBuffer buffer(Record record);

    Deserializer<?> deserializer(RecordTransformation transformation);
    Serializer<?> serializer(RecordTransformation transformation);
    List<Mapper<?, ?>> mappers(RecordTransformation transformation);
    SchemaTransformation schemaTransformation(RecordTransformation transformation);
    InputSchemaIdentification inputSchemaIdentification(RecordTransformation transformation);
    OutputSchemaIdentification outputSchemaIdentification(RecordTransformation transformation);

    record KeyDataLocation() implements RecordDataLocation {

        public static final KeyDataLocation INSTANCE = new KeyDataLocation();

        @Override
        public @Nullable ByteBuffer buffer(Record record) {
            return record.key();
        }

        @Override
        public Deserializer<?> deserializer(RecordTransformation transformation) {
            return transformation.keyDeserializer();
        }

        @Override
        public Serializer<?> serializer(RecordTransformation transformation) {
            return transformation.keySerializer();
        }

        @Override
        public List<Mapper<?, ?>> mappers(RecordTransformation transformation) {
            return transformation.keyMappers();
        }

        @Override
        public SchemaTransformation schemaTransformation(RecordTransformation transformation) {
            return transformation.keySchemaTransformation();
        }

        @Override
        public InputSchemaIdentification inputSchemaIdentification(RecordTransformation transformation) {
            return transformation.keyInputSchemaIdentification();
        }

        @Override
        public OutputSchemaIdentification outputSchemaIdentification(RecordTransformation transformation) {
            return transformation.keyOutputschemaIdentification();
        }
    }

    record ValueDataLocation() implements RecordDataLocation {

        public static final ValueDataLocation INSTANCE = new ValueDataLocation();

        @Override
        public @Nullable ByteBuffer buffer(Record record) {
            return record.value();
        }

        @Override
        public Deserializer<?> deserializer(RecordTransformation transformation) {
            return transformation.valueDeserializer();
        }

        @Override
        public Serializer<?> serializer(RecordTransformation transformation) {
            return transformation.valueSerializer();
        }

        @Override
        public List<Mapper<?, ?>> mappers(RecordTransformation transformation) {
            return transformation.valueMappers();
        }

        @Override
        public SchemaTransformation schemaTransformation(RecordTransformation transformation) {
            return transformation.valueSchemaTransformation();
        }

        @Override
        public InputSchemaIdentification inputSchemaIdentification(RecordTransformation transformation) {
            return transformation.valueInputSchemaIdentification();
        }

        @Override
        public OutputSchemaIdentification outputSchemaIdentification(RecordTransformation transformation) {
            return transformation.valueOutputSchemaIdentification();
        }
    }
}


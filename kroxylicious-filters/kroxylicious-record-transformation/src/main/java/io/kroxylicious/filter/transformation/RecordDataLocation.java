/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.transformation;

import java.nio.ByteBuffer;

import org.apache.kafka.common.record.Record;

import io.kroxylicious.filter.transformation.api.format.Deserializer;
import io.kroxylicious.filter.transformation.api.format.Serializer;
import io.kroxylicious.filter.transformation.api.mapper.Mapper;
import io.kroxylicious.filter.transformation.api.mapper.Mappers;
import io.kroxylicious.filter.transformation.api.schema.identification.OutputSchemaIdentification;
import io.kroxylicious.filter.transformation.api.schema.identification.WireSchemaId;

import edu.umd.cs.findbugs.annotations.Nullable;

/**
 * Enumerates the places within a Kafka record where some data might be located.
 */
public sealed interface RecordDataLocation permits RecordDataLocation.KeyDataLocation, RecordDataLocation.ValueDataLocation {

    @Nullable ByteBuffer buffer(Record record);

    Deserializer<?> deserializer(RecordTransform transformation);
    Serializer<?> serializer(RecordTransform transformation);
    Mapper<?, ?> mappers(RecordTransform transformation);
    Mapper<WireSchemaId, WireSchemaId> schemaTransformation(RecordTransform transformation);
    Deserializer<? extends WireSchemaId> inputSchemaIdentification(RecordTransform transformation);
    OutputSchemaIdentification outputSchemaIdentification(RecordTransform transformation);

    record KeyDataLocation() implements RecordDataLocation {

        public static final KeyDataLocation INSTANCE = new KeyDataLocation();

        @Override
        public @Nullable ByteBuffer buffer(Record record) {
            return record.key();
        }

        @Override
        public Deserializer<?> deserializer(RecordTransform transformation) {
            return transformation.keyTransform().deserializer();
        }

        @Override
        public Serializer<?> serializer(RecordTransform transformation) {
            return transformation.keyTransform().serializer();
        }

        @Override
        public Mapper<?, ?> mappers(RecordTransform transformation) {
            return transformation.keyTransform().mapperOpt().orElse(Mappers.identity(transformation.keyTransform().deserializer().returnedType()));
        }

        @Override
        public Mapper schemaTransformation(RecordTransform transformation) {
            return transformation.keySchemaIdTransform().schemaIdTransformation();
        }

        @Override
        public Deserializer inputSchemaIdentification(RecordTransform transformation) {
            return transformation.keySchemaIdTransform().inputSchemaIdentification();
        }

        @Override
        public OutputSchemaIdentification outputSchemaIdentification(RecordTransform transformation) {
            return transformation.keySchemaIdTransform().outputschemaIdentification();
        }
    }

    record ValueDataLocation() implements RecordDataLocation {

        public static final ValueDataLocation INSTANCE = new ValueDataLocation();

        @Override
        public @Nullable ByteBuffer buffer(Record record) {
            return record.value();
        }

        @Override
        public Deserializer<?> deserializer(RecordTransform transformation) {
            return transformation.valueTransform().deserializer();
        }

        @Override
        public Serializer<?> serializer(RecordTransform transformation) {
            return transformation.valueTransform().serializer();
        }

        @Override
        public Mapper<?, ?> mappers(RecordTransform transformation) {
            return transformation.valueTransform().mapperOpt().orElse(Mappers.identity(transformation.valueTransform().deserializer().returnedType()));
        }

        @Override
        public Mapper schemaTransformation(RecordTransform transformation) {
            return transformation.valueSchemaIdTransform().schemaIdTransformation();
        }

        @Override
        public Deserializer inputSchemaIdentification(RecordTransform transformation) {
            return transformation.valueSchemaIdTransform().inputSchemaIdentification();
        }

        @Override
        public OutputSchemaIdentification<?> outputSchemaIdentification(RecordTransform transformation) {
            return transformation.valueSchemaIdTransform().outputschemaIdentification();
        }
    }
}


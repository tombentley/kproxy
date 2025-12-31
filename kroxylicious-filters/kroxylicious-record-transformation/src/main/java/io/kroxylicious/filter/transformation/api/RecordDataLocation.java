/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.transformation.api;

import java.nio.ByteBuffer;

import org.apache.kafka.common.record.Record;

import io.kroxylicious.filter.transformation.model.DataTransform;
import io.kroxylicious.filter.transformation.model.RecordTransform;

import edu.umd.cs.findbugs.annotations.Nullable;

/**
 * Enumerates the places within a Kafka record where some data might be located.
 */
public sealed interface RecordDataLocation permits RecordDataLocation.KeyDataLocation, RecordDataLocation.ValueDataLocation {

    KeyDataLocation KEY = new KeyDataLocation();
    ValueDataLocation VALUE = new ValueDataLocation();

    /**
     * @param record The record
     * @return The buffer of data at this location, or null
     */
    @Nullable ByteBuffer buffer(Record record);

    /**
     * @param transformation The transform
     * @return The transformation to be applied at this location
     */
    DataTransform<?, ?, ?, ?, ?, ?> dataTransform(RecordTransform transformation);

    record KeyDataLocation() implements RecordDataLocation {

        @Override
        public @Nullable ByteBuffer buffer(Record record) {
            return record.key();
        }

        @Override
        public DataTransform<?, ?, ?, ?, ?, ?> dataTransform(RecordTransform transformation) {
            return transformation.keyTransform();
        }
    }

    record ValueDataLocation() implements RecordDataLocation {

        @Override
        public @Nullable ByteBuffer buffer(Record record) {
            return record.value();
        }

        @Override
        public DataTransform<?, ?, ?, ?, ?, ?> dataTransform(RecordTransform transformation) {
            return transformation.valueTransform();
        }


    }
}


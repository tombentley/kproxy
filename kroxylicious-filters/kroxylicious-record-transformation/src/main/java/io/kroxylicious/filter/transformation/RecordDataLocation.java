/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.transformation;

import java.nio.ByteBuffer;

import org.apache.kafka.common.record.Record;

import io.kroxylicious.filter.transformation.api.schema.identification.InputSchemaIdentification;
import io.kroxylicious.filter.transformation.api.schema.identification.OutputSchemaIdentification;
import io.kroxylicious.filter.transformation.api.schema.identification.SchemaTransformation;

/**
 * Enumerates the places within a Kafka record where some data might be located.
 */
public sealed interface RecordDataLocation permits KeyDataLocation, ValueDataLocation {

    ByteBuffer buffer(Record record);

    DataTransformation dataTransformation(RecordTransformation transformation);
    SchemaTransformation schemaTransformation(RecordTransformation transformation);
    InputSchemaIdentification inputSchemaIdentification(RecordTransformation transformation);
    OutputSchemaIdentification outputSchemaIdentification(RecordTransformation transformation);
}


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

public record KeyDataLocation() implements RecordDataLocation {

    public static final KeyDataLocation INSTANCE = new KeyDataLocation();

    @Override
    public ByteBuffer buffer(Record record) {
        return record.key();
    }

    @Override
    public DataTransformation dataTransformation(RecordTransformation transformation) {
        return transformation.keyTransformation();
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

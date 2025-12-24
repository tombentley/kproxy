/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.transformation;

import io.kroxylicious.filter.transformation.api.schema.identification.InputSchemaIdentification;
import io.kroxylicious.filter.transformation.api.schema.identification.OutputSchemaIdentification;
import io.kroxylicious.filter.transformation.api.schema.identification.SchemaTransformation;

/**
 * A collection of transformations to be applied to a record.
 * @param headerTransformation The transformation to apply to each record's headers.
 * @param keyTransformation The transformation to apply to each record's key.
 * @param valueTransformation The transformation to apply to each record's value.
 */
public record RecordTransformation(

        HeadersTransformation headerTransformation,

        InputSchemaIdentification keyInputSchemaIdentification,
        SchemaTransformation keySchemaTransformation,
        OutputSchemaIdentification keyOutputschemaIdentification,
        DataTransformation keyTransformation,

        InputSchemaIdentification valueInputSchemaIdentification,
        SchemaTransformation valueSchemaTransformation,
        OutputSchemaIdentification valueOutputSchemaIdentification,
        DataTransformation valueTransformation
        ) {
}

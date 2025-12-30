/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.transformation;

import io.kroxylicious.filter.transformation.api.mapper.HeaderMapping;
import io.kroxylicious.filter.transformation.api.schema.identification.WireSchemaId;

/**
 * A collection of transformations to be applied to a record.
 */
public record RecordTransform(

        HeaderMapping headerTransformation,

        SchemaIdTransform<WireSchemaId, WireSchemaId> keySchemaIdTransform,
        SchemalessDataTransform keyTransform,

        SchemaIdTransform<WireSchemaId, WireSchemaId> valueSchemaIdTransform,
        SchemalessDataTransform valueTransform
        ) {

}

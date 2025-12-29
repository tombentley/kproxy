/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.transformation;

import java.util.List;

import org.apache.kafka.common.header.Header;

import io.kroxylicious.filter.transformation.api.mapper.Mapper;
import io.kroxylicious.filter.transformation.api.schema.identification.WireSchemaId;

/**
 * A collection of transformations to be applied to a record.
 */
public record RecordTransform(

        Mapper<List<Header>, List<Header>> headerTransformation,

        SchemaIdTransform<WireSchemaId, WireSchemaId> keySchemaIdTransform,
        DataTransform keyTransform,

        SchemaIdTransform<WireSchemaId, WireSchemaId> valueSchemaIdTransform,
        DataTransform valueTransform
        ) {

}

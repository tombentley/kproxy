/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.transformation.model;

import java.util.Optional;

import io.kroxylicious.filter.transformation.api.mapper.DataMapping;
import io.kroxylicious.filter.transformation.api.schema.identification.SchemaIdSerializer;
import io.kroxylicious.filter.transformation.api.schema.identification.SchemaIdDeserializer;
import io.kroxylicious.filter.transformation.api.schema.identification.WireSchemaId;

public sealed interface DataTransform<W extends WireSchemaId, S, T,
        W2 extends WireSchemaId, S2, T2> permits SchemalessDataTransform, LateBoundDataTransform {
    SchemaIdDeserializer schemaIdDeserializer();
    Optional<DataMapping<W, S, T, W2, S2, T2>> mapperOpt();
    SchemaIdSerializer<W2> schemaIdSerializer();
}

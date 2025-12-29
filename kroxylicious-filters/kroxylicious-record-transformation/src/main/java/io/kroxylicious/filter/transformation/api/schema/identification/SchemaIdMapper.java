/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.transformation.api.schema.identification;

import io.kroxylicious.filter.transformation.api.mapper.Mapper;

public interface SchemaIdMapper<S extends WireSchemaId,
        W extends WireSchemaId> extends Mapper<S, W> {


}



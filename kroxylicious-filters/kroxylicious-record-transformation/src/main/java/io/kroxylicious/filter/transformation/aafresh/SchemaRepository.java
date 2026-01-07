/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.transformation.aafresh;

import java.util.concurrent.CompletionStage;

import io.kroxylicious.filter.transformation.api.schema.registry.ResolvedSchema;

/**
 * A store of schemas.
 */
public interface SchemaRepository {
    CompletionStage<ResolvedSchema> getSchema(String schemaId);
}

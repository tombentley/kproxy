/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.transformation.aafresh;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import io.kroxylicious.filter.transformation.api.schema.registry.ResolvedSchema;
import io.kroxylicious.filter.transformation.api.schema.registry.UnsupportedSchemaIdTypeException;

public class InMemorySchemaRepository implements SchemaRepository {
    private final Map<String, ResolvedSchema> schemaMap;

    public InMemorySchemaRepository(Map<String, ResolvedSchema> schemaMap) {
        this.schemaMap = schemaMap;
    }

    @Override
    public CompletionStage<ResolvedSchema> getSchema(String schemaId) {
        ResolvedSchema resolvedSchema = schemaMap.get(schemaId);
        if (resolvedSchema == null) {
            return CompletableFuture.completedFuture(null);
        }
        return CompletableFuture.completedFuture(resolvedSchema);
    }
}

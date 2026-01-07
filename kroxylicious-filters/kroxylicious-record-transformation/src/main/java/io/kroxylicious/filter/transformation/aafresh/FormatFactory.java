/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.transformation.aafresh;

import java.util.Map;
import java.util.concurrent.CompletionStage;

import io.kroxylicious.filter.transformation.api.schema.registry.ResolvedSchema;

public class FormatFactory {

    private final Map<String, SchemaRepository> registryMap;

    private final Map<String, SchemaFactory> schemaFactories;

    public FormatFactory(Map<String, SchemaRepository> registryMap,
                         Map<String, SchemaFactory> schemaFactories) {
        this.registryMap = registryMap;
        this.schemaFactories = schemaFactories;
    }

    public CompletionStage<Format<?>> resolve(FormatDescriptor descriptor) {
        SchemaRepository schemaRepository = registryMap.get(descriptor.repositoryId());
        if (schemaRepository == null) {
            throw new RuntimeException("No such registry: " + descriptor.repositoryId());
        }
        // SchemaRepository abstracts the REST interaction
        CompletionStage<ResolvedSchema> schemaFuture = schemaRepository.getSchema(descriptor.schemaId());
        return schemaFuture.thenApply(resolvedSchema -> {
            SchemaFactory<?> schemaFactory = schemaFactories.get(resolvedSchema.schemaType());
            if (schemaFactory == null) {
                throw new RuntimeException("No such schemaFactory: " + resolvedSchema.schemaType());
            }
            Object schema = schemaFactory.parseSchema(resolvedSchema.schema());
            return new Format<>(descriptor.formatName(), descriptor.encoding(), schema);
        });
    }
}

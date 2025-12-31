/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.transformation.api.schema.registry;

import java.util.concurrent.CompletionStage;

import io.kroxylicious.filter.transformation.api.schema.identification.WireSchemaId;

public interface SchemaRegistry {

    /**
     * @param schemaIdType A schema id type
     * @return true iff this registry implementation supports the given kind of schema id
     */
    boolean supports(Class<? extends WireSchemaId> schemaIdType);

    /**
     * Returns a completion stage for the resolved schema for the given schema id.
     * This method should throw UnsupportedSchemaIdTypeException immediately
     * if the given kind of schema id is not supported, rather than
     * returning a failed stage.
     * @param wireSchemaId The schema id to get
     * @return A completion stage. The returned stage should complete with the
     * resolved schema, or complete with null if a schema for the given schema id was not found.
     * The returned stage may also complete exceptionally.
     * @throws UnsupportedSchemaIdTypeException If kind of schema id given is
     * not supported by this registry
     */
    CompletionStage<ResolvedSchema> getSchema(WireSchemaId wireSchemaId)
            throws UnsupportedSchemaIdTypeException;

}

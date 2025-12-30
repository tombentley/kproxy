/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.transformation.api.schema.registry;

import java.util.concurrent.CompletionStage;

import io.kroxylicious.filter.transformation.api.schema.identification.WireSchemaId;

public interface SchemaRegistry {

    // TODO the registry is presumably returning some metadata about the type of schema returned
    //   the impl of this interface is best placed to interpret that and return a deserializer
    <W extends WireSchemaId> CompletionStage<ResolvedSchema> getSchema(W wireSchemaId);

}

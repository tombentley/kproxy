/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.transformation.api;

import io.kroxylicious.filter.transformation.api.schema.identification.WireSchemaId;

import edu.umd.cs.findbugs.annotations.Nullable;

public record SchemaAndValue<W extends WireSchemaId, S, V>(W schemaId, @Nullable S schema, @Nullable V value) {

    public <W2 extends WireSchemaId> SchemaAndValue<W2, S, V> withSchemaId(W2 newSchemaId) {
        return new SchemaAndValue<>(newSchemaId, schema, value);
    }

}

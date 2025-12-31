/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.transformation.api;

import java.io.InputStream;
import java.util.Objects;

import io.kroxylicious.filter.transformation.TransformationInputStream;
import io.kroxylicious.filter.transformation.api.schema.identification.NoSchemaId;
import io.kroxylicious.filter.transformation.api.schema.identification.WireSchemaId;

public record Type<W extends WireSchemaId, S, V>(Class<W> wireSchemaId, Class<S> schema, Class<V> cls) {
    public Type {
        Objects.requireNonNull(wireSchemaId);
        Objects.requireNonNull(schema);
        Objects.requireNonNull(cls);
    }

    public static Type<NoSchemaId, Void, InputStream> fromBytes() {
        return new Type(NoSchemaId.class, Void.class, TransformationInputStream.class);
    }
}

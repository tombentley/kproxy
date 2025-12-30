/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.transformation.api;

import java.io.InputStream;

import io.kroxylicious.filter.transformation.api.schema.identification.NoSchema;
import io.kroxylicious.filter.transformation.api.schema.identification.WireSchemaId;

public record Type<W extends WireSchemaId, S, T>(Class<W> wireSchemaId, Class<S> schema, Class<T> cls) {
    public static Type<NoSchema, Void, InputStream> fromBytes() {
        return new Type(NoSchema.class, null, InputStream.class);
    }
}

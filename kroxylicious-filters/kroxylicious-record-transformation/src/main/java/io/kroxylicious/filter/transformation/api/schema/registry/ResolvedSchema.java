/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.transformation.api.schema.registry;

import java.util.Arrays;
import java.util.HexFormat;
import java.util.Objects;

import io.kroxylicious.filter.transformation.api.schema.identification.WireSchemaId;

public record ResolvedSchema(WireSchemaId schemaId,
                             String schemaType,
                             byte[] schema) {
    public ResolvedSchema {
        Objects.requireNonNull(schemaId);
        Objects.requireNonNull(schema);
        Objects.requireNonNull(schemaType);
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof ResolvedSchema that)) {
            return false;
        }
        return Objects.equals(schemaType, that.schemaType) && Objects.deepEquals(schema, that.schema) && Objects.equals(schemaId, that.schemaId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(schemaId, Arrays.hashCode(schema), schemaType);
    }

    @Override
    public String toString() {
        return "ResolvedSchema{" +
                "schemaId=" + schemaId +
                ", type='" + schemaType + '\'' +
                ", schema=" + HexFormat.of().formatHex(schema) +
                '}';
    }
}

/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.transformation.aafresh;

import java.util.Objects;

import edu.umd.cs.findbugs.annotations.Nullable;

/**
 * A description of a {@linkplain Format concrete data format} that be passed
 * to a {@link FormatFactory}.
 *
 * @param formatName The name of the abstract format (e.g. "json" or "avro")
 * @param encoding The format encoding. Legal values depend on the format name. The encoding serves to
 * distinguish formats which can be encoded in different ways.
 * For example, Avro can be encoded as binary or JSON. Another example: ASN.1 supports a wide variety of encoding rules,
 * such a BER and DER.
 * @param repositoryId The identifier of a schema repository
 * which holds the schema, if the format depends on a schema.
 * Or null if the format does not depend on a schema (e.g. JSON).
 * @param schemaId The identifier of the schema within the schema repository identified by registryId,
 * if the format depends on a schema.
 * Or null if the format does not depend on a schema (e.g. JSON).
 */
public record FormatDescriptor(String formatName,
                               String encoding,
                               @Nullable String repositoryId,
                               @Nullable String schemaId) {
    public FormatDescriptor {
        Objects.requireNonNull(formatName);
        Objects.requireNonNull(encoding);
        if ((repositoryId == null) != (schemaId == null)) {
            throw new IllegalArgumentException("registryId and schemaId must either both be null, or both be non-null");
        }
    }
}

/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.record.manipulation.filter.resolve;

import edu.umd.cs.findbugs.annotations.Nullable;

/**
 * Extracts the lookup key (id) of a "related entity" from a record.
 *
 * <p>This is the first of the three collaborators that make up a {@link Facet}: given a record it
 * returns the id that identifies the related entity (e.g. a schema {@code globalId} extracted from
 * the magic-byte framing of the record key/value, or an EDEK read from a header). Returning
 * {@code null} means this record does not reference an entity of this kind.</p>
 *
 * @param <ID> the type of the lookup id.
 */
@FunctionalInterface
public interface LookupKeyExtractor<R, ID> {

    /**
     * Extract the lookup id from the given record.
     * @param record the record.
     * @return the id, or {@code null} if this record references no entity of this kind.
     */
    @Nullable
    ID extract(R record);
}

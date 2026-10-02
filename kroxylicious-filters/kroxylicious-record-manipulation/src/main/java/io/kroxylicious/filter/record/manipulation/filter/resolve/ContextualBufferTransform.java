/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.record.manipulation.filter.resolve;

import java.nio.ByteBuffer;

import io.kroxylicious.kafka.common.record.internal.Record;
import io.kroxylicious.transform.service.BufferTransform;

import edu.umd.cs.findbugs.annotations.Nullable;

/**
 * A record buffer transform that, unlike the plain {@link BufferTransform}, is given the
 * {@link ResolvedEntities} for the request so it can consume resolved "related things" (schema
 * content, DEKs, ...).
 *
 * <p>This is how a {@code then} clause "participates" in the collection of related entities: a
 * schema-aware writer looks up the record's schema content from {@link ResolvedEntities} rather than
 * being handed only the {@link Record}. Transforms that need no resolved context can be adapted from a
 * plain {@link BufferTransform} via {@link #of(BufferTransform)}.</p>
 */
@FunctionalInterface
public interface ContextualBufferTransform {

    /**
     * Compute the new buffer for the given record.
     * @param record the record.
     * @param resolved the entities resolved for this request.
     * @return the new buffer, or {@code null}.
     */
    @Nullable
    ByteBuffer apply(Record record, ResolvedEntities resolved);

    /**
     * Adapt a plain {@link BufferTransform} into a {@link ContextualBufferTransform} that ignores the
     * resolved context.
     * @param delegate the plain transform.
     * @return a contextual transform delegating to {@code delegate}.
     */
    static ContextualBufferTransform of(BufferTransform delegate) {
        return (record, resolved) -> delegate.apply(new RecordWrapper(record), null);
    }
}

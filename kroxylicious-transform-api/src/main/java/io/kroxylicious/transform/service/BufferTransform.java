/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.transform.service;

import java.nio.ByteBuffer;
import java.util.Set;

import io.kroxylicious.transform.records.RecordView;

import edu.umd.cs.findbugs.annotations.Nullable;

/**
 * Transforms a single record buffer (a key or a value) to a new buffer.
 *
 * <p>Operates on the engine's read-only {@link RecordView}, which is the single record abstraction
 * shared by the whole transform API; there is no separate "service" record type.</p>
 */
public interface BufferTransform {

    /**
     * {@return the per-record dependencies this transform needs resolved before {@link #apply} is called}
     * The environment resolves these and supplies them via the {@link Dependencies} passed to
     * {@link #apply(RecordView, Dependencies)}.
     */
    default Set<DependencyKey<?, ?>> dependencies() {
        return Set.of();
    }

    /**
     * Applies the transform with resolved per-record dependencies available. The default ignores the
     * dependencies and delegates to {@link #apply(RecordView)}; dependency-consuming transforms override this.
     * @param record the record to read from.
     * @param dependencies the resolved per-record dependencies.
     * @return the new buffer, or {@code null}.
     */
    @Nullable
    default ByteBuffer apply(RecordView record, Dependencies dependencies) {
        return apply(record);
    }

    /**
     * Applies the transform.
     * @param record the record to read from.
     * @return the new buffer, or {@code null}.
     */
    @Nullable
    ByteBuffer apply(RecordView record);
}

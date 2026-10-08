/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.transform.service;

import java.nio.ByteBuffer;

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
     * Applies the transform.
     * @param record the record to read from.
     * @return the new buffer, or {@code null}.
     */
    @Nullable
    ByteBuffer apply(RecordView record);
}

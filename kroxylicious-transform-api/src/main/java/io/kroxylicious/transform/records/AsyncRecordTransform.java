/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.transform.records;

import java.util.concurrent.CompletionStage;

/**
 * The asynchronous sibling of {@link RecordTransform}: computes a {@link RecordEdit} for each
 * record on the produce path, returning a {@link CompletionStage} so the edit may depend on an asynchronous
 * per-record lookup (for example a schema from a registry, or a key from a KMS).
 *
 * <p>Transforms that resolve synchronously (for example from a warm cache, or a {@code keep()}) should return
 * an already-completed stage (via {@link java.util.concurrent.CompletableFuture#completedFuture}); the
 * bounded-window driver recognises these and streams them through without consuming a window slot.</p>
 */
@FunctionalInterface
public interface AsyncRecordTransform {

    /**
     * Computes the edit to apply to the given record.
     * @param batch the batch the record belongs to
     * @param record the record to transform
     * @return a stage completing with the edit to apply (use {@link RecordEdit#keep()} for no change)
     */
    CompletionStage<RecordEdit> transform(BatchView batch, RecordView record);
}

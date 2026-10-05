/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.transform.records;

/**
 * Computes a {@link BatchEdit} for each batch. Batch-level edits (compression, batch-wide timestamp transforms)
 * are an independent axis from per-record edits and compose with them.
 */
@FunctionalInterface
public interface BatchTransform {

    /**
     * Computes the edit to apply to the given batch.
     * @param batch the batch to transform
     * @return the edit to apply (use {@link BatchEdit#keep()} for no change)
     */
    BatchEdit transform(BatchView batch);

    /**
     * {@return a transform that keeps every batch unchanged}
     */
    static BatchTransform keep() {
        return batch -> BatchEdit.keep();
    }
}

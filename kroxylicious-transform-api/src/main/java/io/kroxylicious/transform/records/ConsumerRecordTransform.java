/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.transform.records;

/**
 * Computes a {@link ConsumerRecordEdit} for each record on the consume path (fetch / share-fetch).
 */
@FunctionalInterface
public interface ConsumerRecordTransform {

    /**
     * Computes the edit to apply to the given record.
     * @param batch the batch the record belongs to
     * @param record the record to transform
     * @return the edit to apply (use {@link ConsumerRecordEdit#keep()} for no change, or
     *         {@link ConsumerRecordEdit#drop()} to remove the record)
     */
    ConsumerRecordEdit transform(BatchView batch, RecordView record);
}

/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.transform.records;

/**
 * Computes a {@link ProducerRecordEdit} for each record on the produce path.
 */
@FunctionalInterface
public interface ProducerRecordTransform {

    /**
     * Computes the edit to apply to the given record.
     * @param batch the batch the record belongs to
     * @param record the record to transform
     * @return the edit to apply (use {@link ProducerRecordEdit#keep()} for no change)
     */
    ProducerRecordEdit transform(BatchView batch, RecordView record);
}

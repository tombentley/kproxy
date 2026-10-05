/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.transform.records;

import io.kroxylicious.kafka.common.record.TimestampType;
import io.kroxylicious.kafka.common.record.internal.CompressionType;

/**
 * An immutable, read-only view of a single record batch: its (immutable) framing metadata plus lazy access to
 * the records it contains. Iterating {@link #records()} decodes — and, for a compressed batch, decompresses —
 * the records.
 */
public interface BatchView {

    /**
     * {@return the base offset of the batch}
     */
    long baseOffset();

    /**
     * {@return the last offset of the batch (inclusive)}
     */
    long lastOffset();

    /**
     * {@return the magic (record format version) of the batch}
     */
    byte magic();

    /**
     * {@return the max timestamp of the batch}
     */
    long maxTimestamp();

    /**
     * {@return the timestamp type of the batch}
     */
    TimestampType timestampType();

    /**
     * {@return the compression codec of the batch}
     */
    CompressionType compression();

    /**
     * {@return the producer id, or -1 if there is none}
     */
    long producerId();

    /**
     * {@return the producer epoch, or -1 if there is none}
     */
    short producerEpoch();

    /**
     * {@return the base sequence number, or -1 if there is none}
     */
    int baseSequence();

    /**
     * {@return the partition leader epoch, or -1 if unknown}
     */
    int partitionLeaderEpoch();

    /**
     * {@return true if the batch is transactional}
     */
    boolean isTransactional();

    /**
     * {@return true if this is a control batch (never handed to a record transform)}
     */
    boolean isControlBatch();

    /**
     * {@return the number of records in the batch}
     */
    int numRecords();

    /**
     * {@return the records of the batch, in order}
     */
    Iterable<RecordView> records();
}

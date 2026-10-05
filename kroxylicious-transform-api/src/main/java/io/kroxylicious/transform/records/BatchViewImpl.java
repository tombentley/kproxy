/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.transform.records;

import java.util.Iterator;

import io.kroxylicious.kafka.common.record.TimestampType;
import io.kroxylicious.kafka.common.record.internal.CompressionType;
import io.kroxylicious.kafka.common.record.internal.Record;
import io.kroxylicious.kafka.common.record.internal.RecordBatch;

/**
 * A {@link BatchView} backed by an inherited {@link RecordBatch}. Iterating {@link #records()} decodes (and, for
 * a compressed batch, decompresses) the records via the batch's own iterator.
 */
final class BatchViewImpl implements BatchView {

    private final RecordBatch batch;

    BatchViewImpl(RecordBatch batch) {
        this.batch = batch;
    }

    @Override
    public long baseOffset() {
        return batch.baseOffset();
    }

    @Override
    public long lastOffset() {
        return batch.lastOffset();
    }

    @Override
    public byte magic() {
        return batch.magic();
    }

    @Override
    public long maxTimestamp() {
        return batch.maxTimestamp();
    }

    @Override
    public TimestampType timestampType() {
        return batch.timestampType();
    }

    @Override
    public CompressionType compression() {
        return batch.compressionType();
    }

    @Override
    public long producerId() {
        return batch.producerId();
    }

    @Override
    public short producerEpoch() {
        return batch.producerEpoch();
    }

    @Override
    public int baseSequence() {
        return batch.baseSequence();
    }

    @Override
    public int partitionLeaderEpoch() {
        return batch.partitionLeaderEpoch();
    }

    @Override
    public boolean isTransactional() {
        return batch.isTransactional();
    }

    @Override
    public boolean isControlBatch() {
        return batch.isControlBatch();
    }

    @Override
    public int numRecords() {
        Integer count = batch.countOrNull();
        return count == null ? 0 : count;
    }

    @Override
    public Iterable<RecordView> records() {
        return () -> new Iterator<>() {
            private final Iterator<Record> delegate = batch.iterator();

            @Override
            public boolean hasNext() {
                return delegate.hasNext();
            }

            @Override
            public RecordView next() {
                return new RecordViewImpl(delegate.next());
            }
        };
    }
}

/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.transform.records;

import java.util.Objects;
import java.util.concurrent.CompletionStage;

import io.kroxylicious.kafka.common.record.internal.BaseRecords;
import io.kroxylicious.kafka.common.record.internal.MemoryRecords;
import io.kroxylicious.kafka.common.utils.ByteBufferOutputStream;

/**
 * <p>A handle over a sequence of record batches from a broker and destined for a consumer.
 * It supports reading the batches/records as immutable {@link BatchView}s and
 * {@link RecordView}s, and applying transforms to produce a new {@link MemoryRecords}.</p>
 *
 * <p>Transforms may be synchronous ({@link #applyFetch}) or asynchronous ({@link #applyFetchAsync});
 * the async terminals drive a bounded window of
 * concurrent per-record transforms via {@link AsyncTransformDriver} while emitting batches in order.</p>
 */
public final class ConsumerRecords extends RecordFrames {

    private ConsumerRecords(MemoryRecords records) {
        super(records);
    }

    public static ConsumerRecords of(BaseRecords records) {
        return new ConsumerRecords((MemoryRecords) records);
    }

    /**
     * Apply a consume-path (fetch / share-fetch) record transform, writing the result into {@code out}.
     * @param transform the record transform
     * @param out the output buffer
     * @return the transformed records
     */
    public MemoryRecords applyFetch(RecordTransform transform,
                                    ByteBufferOutputStream out) {
        return applyFetch(BatchTransform.keep(), transform, out);
    }

    /**
     * Apply a consume-path (fetch / share-fetch) batch and record transform, writing the result into {@code out}.
     * @param batchTransform the batch transform
     * @param transform the record transform
     * @param out the output buffer
     * @return the transformed records
     */
    public MemoryRecords applyFetch(BatchTransform batchTransform,
                                    RecordTransform transform,
                                    ByteBufferOutputStream out) {
        Objects.requireNonNull(batchTransform);
        Objects.requireNonNull(transform);
        return applyInternal(batchTransform, transform::transform, out);
    }

    /**
     * Apply a consume-path (fetch / share-fetch) record transform asynchronously, writing the result into {@code out}.
     * @param transform the async record transform
     * @param out the output buffer
     * @param options the window size and assembly executor
     * @return a stage completing with the transformed records (or completing exceptionally if any record
     *         transform fails)
     */
    public CompletionStage<MemoryRecords> applyFetchAsync(AsyncRecordTransform transform,
                                                          ByteBufferOutputStream out,
                                                          TransformOptions options) {
        return applyFetchAsync(BatchTransform.keep(), transform, out, options);
    }

    /**
     * Apply a consume-path (fetch / share-fetch) batch and record transform asynchronously, writing the result into {@code out}.
     * @param batchTransform the batch transform
     * @param transform the async record transform
     * @param out the output buffer
     * @param options the window size and assembly executor
     * @return a stage completing with the transformed records (or completing exceptionally if any record
     *         transform fails)
     */
    public CompletionStage<MemoryRecords> applyFetchAsync(BatchTransform batchTransform,
                                                          AsyncRecordTransform transform,
                                                          ByteBufferOutputStream out,
                                                          TransformOptions options) {
        Objects.requireNonNull(batchTransform);
        Objects.requireNonNull(transform);
        return applyInternalAsync(batchTransform, transform::transform, out, options);
    }

}

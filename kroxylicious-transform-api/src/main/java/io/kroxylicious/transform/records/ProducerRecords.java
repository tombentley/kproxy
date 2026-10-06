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
 * <p>A handle over a sequence of record batches
 * from a producer and destined for a broker.
 * It supports reading the batches/records as immutable {@link BatchView}s and
 * {@link RecordView}s, and applying transforms to produce a new {@link MemoryRecords}.</p>
 *
 * <p>Transforms may be synchronous ({@link #applyProduce}) or asynchronous ({@link #applyProduceAsync});
 * the async terminals drive a bounded window of
 * concurrent per-record transforms via {@link AsyncTransformDriver} while emitting batches in order.</p>
 */
public final class ProducerRecords extends RecordFrames {

    private ProducerRecords(MemoryRecords records) {
        super(records);
    }

    public static ProducerRecords of(BaseRecords records) {
        return new ProducerRecords((MemoryRecords) records);
    }

    /**
     * Apply a produce-path record transform, writing the result into {@code out}.
     * @param transform the record transform
     * @param out the output buffer
     * @return the transformed records
     */
    public MemoryRecords applyProduce(ProducerRecordTransform transform,
                                      ByteBufferOutputStream out) {
        return applyProduce(BatchTransform.keep(), transform, out);
    }

    /**
     * Apply a produce-path batch and record transform, writing the result into {@code out}.
     * @param batchTransform the batch transform
     * @param transform the record transform
     * @param out the output buffer
     * @return the transformed records
     */
    public MemoryRecords applyProduce(BatchTransform batchTransform,
                                      ProducerRecordTransform transform,
                                      ByteBufferOutputStream out) {
        Objects.requireNonNull(batchTransform);
        Objects.requireNonNull(transform);
        return applyInternal(batchTransform, (batch, record) -> {
            ProducerRecordEdit edit = transform.transform(batch, record);
            return new InternalEdit(edit.isKeep(), false, edit.fields());
        }, out);
    }

    /**
     * Apply a produce-path record transform asynchronously, writing the result into {@code out}.
     * @param transform the async record transform
     * @param out the output buffer
     * @param options the window size and assembly executor
     * @return a stage completing with the transformed records (or completing exceptionally if any record
     *         transform fails)
     */
    public CompletionStage<MemoryRecords> applyProduceAsync(AsyncProducerRecordTransform transform,
                                                            ByteBufferOutputStream out,
                                                            TransformOptions options) {
        return applyProduceAsync(BatchTransform.keep(), transform, out, options);
    }

    /**
     * Apply a produce-path batch and record transform asynchronously, writing the result into {@code out}.
     * @param batchTransform the batch transform
     * @param transform the async record transform
     * @param out the output buffer
     * @param options the window size and assembly executor
     * @return a stage completing with the transformed records (or completing exceptionally if any record
     *         transform fails)
     */
    public CompletionStage<MemoryRecords> applyProduceAsync(BatchTransform batchTransform,
                                                            AsyncProducerRecordTransform transform,
                                                            ByteBufferOutputStream out,
                                                            TransformOptions options) {
        Objects.requireNonNull(batchTransform);
        Objects.requireNonNull(transform);
        return applyInternalAsync(batchTransform, (batch, record) -> transform.transform(batch, record)
                .thenApply(edit -> new InternalEdit(edit.isKeep(), false, edit.fields())), out, options);
    }

}

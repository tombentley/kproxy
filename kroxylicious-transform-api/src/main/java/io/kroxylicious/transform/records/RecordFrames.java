/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.transform.records;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;

import io.kroxylicious.kafka.common.compress.Compression;
import io.kroxylicious.kafka.common.header.Header;
import io.kroxylicious.kafka.common.record.TimestampType;
import io.kroxylicious.kafka.common.record.internal.CompressionType;
import io.kroxylicious.kafka.common.record.internal.MemoryRecords;
import io.kroxylicious.kafka.common.record.internal.MemoryRecordsBuilder;
import io.kroxylicious.kafka.common.record.internal.MutableRecordBatch;
import io.kroxylicious.kafka.common.record.internal.RecordBatch;
import io.kroxylicious.kafka.common.utils.ByteBufferOutputStream;

/**
 * <p>A handle over a sequence of record batches (a proxy-friendly replacement for working with
 * {@code MemoryRecords} directly). It supports reading the batches/records as immutable {@link BatchView}s and
 * {@link RecordView}s, and applying directional transforms to produce a new {@link MemoryRecords}.</p>
 *
 * <p>This is a proof-of-concept: it is a thin layer over the inherited {@code MemoryRecords} reader and uses
 * {@code MemoryRecordsBuilder} as the batch encoder. It implements the key "pristine copy" fast path — a batch
 * with no record edits and no batch edit is copied through verbatim (header, CRC and all) — while edited
 * batches are re-encoded. Deliberately out of scope for this POC: a bespoke low-level encoder, per-record byte
 * splicing within edited batches, the async bounded-window driver, and empty-batch retention when every record
 * in a batch is dropped (see TODO in {@link #applyInternal}).</p>
 */
public final class RecordFrames {

    private final MemoryRecords records;

    private RecordFrames(MemoryRecords records) {
        this.records = Objects.requireNonNull(records);
    }

    /**
     * Wraps the given memory records.
     * @param records the inherited memory records to wrap
     * @return a {@link RecordFrames} over the given records
     */
    public static RecordFrames of(MemoryRecords records) {
        return new RecordFrames(records);
    }

    /**
     * {@return the batches, as immutable views, in order (control and empty batches are included)}
     */
    public Iterable<BatchView> batches() {
        return () -> new Iterator<>() {
            private final Iterator<MutableRecordBatch> delegate = records.batches().iterator();

            @Override
            public boolean hasNext() {
                return delegate.hasNext();
            }

            @Override
            public BatchView next() {
                return new BatchViewImpl(delegate.next());
            }
        };
    }

    /**
     * Apply a produce-path record transform, writing the result into {@code out}.
     * @param transform the record transform
     * @param out the output buffer
     * @return the transformed records
     */
    public MemoryRecords applyProduce(ProducerRecordTransform transform, ByteBufferOutputStream out) {
        return applyProduce(BatchTransform.keep(), transform, out);
    }

    /**
     * Apply a produce-path batch and record transform, writing the result into {@code out}.
     * @param batchTransform the batch transform
     * @param transform the record transform
     * @param out the output buffer
     * @return the transformed records
     */
    public MemoryRecords applyProduce(BatchTransform batchTransform, ProducerRecordTransform transform, ByteBufferOutputStream out) {
        Objects.requireNonNull(batchTransform);
        Objects.requireNonNull(transform);
        return applyInternal(batchTransform, (batch, record) -> {
            ProducerRecordEdit edit = transform.transform(batch, record);
            return new InternalEdit(edit.isKeep(), false, edit.fields());
        }, out);
    }

    /**
     * Apply a consume-path (fetch / share-fetch) record transform, writing the result into {@code out}.
     * @param transform the record transform
     * @param out the output buffer
     * @return the transformed records
     */
    public MemoryRecords applyFetch(ConsumerRecordTransform transform, ByteBufferOutputStream out) {
        return applyFetch(BatchTransform.keep(), transform, out);
    }

    /**
     * Apply a consume-path (fetch / share-fetch) batch and record transform, writing the result into {@code out}.
     * @param batchTransform the batch transform
     * @param transform the record transform
     * @param out the output buffer
     * @return the transformed records
     */
    public MemoryRecords applyFetch(BatchTransform batchTransform, ConsumerRecordTransform transform, ByteBufferOutputStream out) {
        Objects.requireNonNull(batchTransform);
        Objects.requireNonNull(transform);
        return applyInternal(batchTransform, (batch, record) -> {
            ConsumerRecordEdit edit = transform.transform(batch, record);
            return new InternalEdit(edit.isKeep(), edit.isDrop(), edit.fields());
        }, out);
    }

    private MemoryRecords applyInternal(BatchTransform batchTransform, InternalRecordTransform transform, ByteBufferOutputStream out) {
        for (MutableRecordBatch batch : records.batches()) {
            if (batch.isControlBatch()) {
                // Control batches are opaque and order-sensitive: copy verbatim, never transform.
                batch.writeTo(out);
                continue;
            }

            BatchView batchView = new BatchViewImpl(batch);
            BatchEdit batchEdit = batchTransform.transform(batchView);

            // Single pass: evaluate every record's edit (iterator() fully materialises, so holding views is safe).
            List<RecordView> views = new ArrayList<>();
            List<InternalEdit> edits = new ArrayList<>();
            boolean modified = !batchEdit.isKeep();
            for (RecordView view : batchView.records()) {
                InternalEdit edit = transform.transform(batchView, view);
                views.add(view);
                edits.add(edit);
                modified |= !edit.keep();
            }

            if (!modified) {
                // Pristine copy: nothing changed, so copy the batch through byte-for-byte.
                batch.writeTo(out);
                continue;
            }

            reencodeBatch(out, batch, batchEdit, views, edits);
        }

        ByteBuffer buffer = out.buffer();
        buffer.flip();
        return MemoryRecords.readableRecords(buffer);
    }

    private static void reencodeBatch(ByteBufferOutputStream out,
                                      MutableRecordBatch batch,
                                      BatchEdit batchEdit,
                                      List<RecordView> views,
                                      List<InternalEdit> edits) {
        CompressionType targetType = batchEdit.compression() != null ? batchEdit.compression() : batch.compressionType();
        Compression compression = Compression.of(targetType).build();
        TimestampType timestampType = batch.timestampType();
        long logAppendTime = timestampType == TimestampType.LOG_APPEND_TIME
                ? batchEdit.transformTimestamp(batch.maxTimestamp())
                : RecordBatch.NO_TIMESTAMP;
        long deleteHorizonMs = batch.deleteHorizonMs().orElse(RecordBatch.NO_TIMESTAMP);

        MemoryRecordsBuilder builder = new MemoryRecordsBuilder(out,
                batch.magic(),
                compression,
                timestampType,
                batch.baseOffset(),
                logAppendTime,
                batch.producerId(),
                batch.producerEpoch(),
                batch.baseSequence(),
                batch.isTransactional(),
                false,
                batch.partitionLeaderEpoch(),
                0,
                deleteHorizonMs);

        int appended = 0;
        for (int i = 0; i < views.size(); i++) {
            InternalEdit edit = edits.get(i);
            if (edit.drop()) {
                continue;
            }
            RecordView view = views.get(i);
            FieldEdits fields = edit.fields();
            ByteBuffer key = fields.resolveKey(view);
            ByteBuffer value = fields.resolveValue(view);
            Header[] headers = fields.resolveHeaders(view);
            long timestamp = batchEdit.transformTimestamp(view.timestamp());
            builder.appendWithOffset(view.offset(), timestamp, key, value, headers);
            appended++;
        }

        if (appended > 0 && batch.magic() >= RecordBatch.MAGIC_VALUE_V2) {
            // Preserve the sequence/offset range even if records were dropped (mirrors log compaction).
            builder.overrideLastOffset(batch.lastOffset());
        }
        // TODO: when appended == 0 (every record dropped) we currently drop the whole batch; to preserve the
        // offset range we should instead retain an empty batch header (RETAIN_EMPTY).
        builder.build();
    }

    private record InternalEdit(boolean keep, boolean drop, FieldEdits fields) {}

    private interface InternalRecordTransform {
        InternalEdit transform(BatchView batch, RecordView record);
    }
}

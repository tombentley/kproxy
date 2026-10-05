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

import io.kroxylicious.kafka.common.record.internal.MemoryRecords;
import io.kroxylicious.kafka.common.record.internal.MutableRecordBatch;
import io.kroxylicious.kafka.common.utils.ByteBufferOutputStream;

/**
 * <p>A handle over a sequence of record batches (a proxy-friendly replacement for working with
 * {@code MemoryRecords} directly). It supports reading the batches/records as immutable {@link BatchView}s and
 * {@link RecordView}s, and applying directional transforms to produce a new {@link MemoryRecords}.</p>
 *
 * <p>This is a proof-of-concept: it is a thin layer over the inherited {@code MemoryRecords} reader and uses
 * {@link BatchEncoder} as the batch encoder. A batch with no record edits and no batch edit is copied through
 * verbatim (header, CRC and all); an edited batch is handed to {@link BatchEncoder}, which splices unedited
 * records byte-for-byte, re-encodes only the edited ones, collapses a uniform timestamp shift to a header-only
 * rewrite, and retains an empty batch header when every record is dropped. Deliberately out of scope for this
 * POC: intra-record field splicing within an edited record, a single decompression pass for compressed batches,
 * and the async bounded-window driver.</p>
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

            BatchEncoder.encode(out, batch, batchEdit, views, edits);
        }

        ByteBuffer buffer = out.buffer();
        buffer.flip();
        return MemoryRecords.readableRecords(buffer);
    }

    record InternalEdit(boolean keep, boolean drop, FieldEdits fields) {}

    private interface InternalRecordTransform {
        InternalEdit transform(BatchView batch, RecordView record);
    }
}

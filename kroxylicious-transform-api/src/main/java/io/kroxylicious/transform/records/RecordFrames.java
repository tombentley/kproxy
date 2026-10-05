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
import java.util.concurrent.CompletionStage;

import io.kroxylicious.kafka.common.record.internal.MemoryRecords;
import io.kroxylicious.kafka.common.record.internal.MutableRecordBatch;
import io.kroxylicious.kafka.common.utils.ByteBufferOutputStream;

/**
 * <p>An abstract handle over a sequence of record batches (a proxy-friendly replacement for working with
 * {@code MemoryRecords} directly).
 * It supports reading the batches/records as immutable {@link BatchView}s and
 * {@link RecordView}s, and applying directional transforms to produce a new {@link MemoryRecords}.
 * There are specific subclasses for the cases of records in a
 * produce request ({@link {@link ProducerRecords}) and a fetch response or a share fetch response
 * ({@link ConsumerRecords}).</p>
 *
 * <p>This is a thin layer over the inherited {@code MemoryRecords} reader and uses
 * {@link BatchEncoder} as the batch encoder. A batch with no record edits and no batch edit is copied through
 * verbatim (header, CRC and all); an edited batch is handed to {@link BatchEncoder}, which splices unedited
 * records byte-for-byte, re-encoding only the edited ones, collapsing a uniform timestamp shift to a header-only
 * rewrite, and retaining an empty batch header when every record is dropped. Transforms may be synchronous
 * or asynchronous; the async terminals drive a bounded window of
 * concurrent per-record transforms via {@link AsyncTransformDriver} while emitting batches in order.</p>
 */
public sealed class RecordFrames permits ProducerRecords, ConsumerRecords {

    private final MemoryRecords records;

    RecordFrames(MemoryRecords records) {
        this.records = Objects.requireNonNull(records);
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

    MemoryRecords applyInternal(BatchTransform batchTransform,
                                InternalRecordTransform transform,
                                ByteBufferOutputStream out) {
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
            for (RecordView view : batchView.records()) {
                views.add(view);
                edits.add(transform.transform(batchView, view));
            }

            encodeBatch(out, batch, batchEdit, views, edits);
        }

        ByteBuffer buffer = out.buffer();
        buffer.flip();
        return MemoryRecords.readableRecords(buffer);
    }

    CompletionStage<MemoryRecords> applyInternalAsync(BatchTransform batchTransform,
                                                      AsyncInternalRecordTransform transform,
                                                      ByteBufferOutputStream out,
                                                      TransformOptions options) {
        Objects.requireNonNull(out);
        Objects.requireNonNull(options);
        return new AsyncTransformDriver(records, batchTransform, transform, out, options).run();
    }

    /**
     * Encode a single (non-control) batch into {@code out}: a pristine batch (no batch edit and every record
     * kept) is copied through byte-for-byte, otherwise it is handed to {@link BatchEncoder}. Shared by the
     * synchronous and asynchronous drivers so both produce byte-identical output for the same edits.
     * @param out the output buffer
     * @param batch the source batch
     * @param batchEdit the batch-level edit
     * @param views the records' views, in batch order
     * @param edits the per-record edits, aligned with {@code views}
     */
    static void encodeBatch(ByteBufferOutputStream out,
                            MutableRecordBatch batch,
                            BatchEdit batchEdit,
                            List<RecordView> views,
                            List<InternalEdit> edits) {
        boolean modified = !batchEdit.isKeep();
        for (InternalEdit edit : edits) {
            modified |= !edit.keep();
        }
        if (!modified) {
            // Pristine copy: nothing changed, so copy the batch through byte-for-byte.
            batch.writeTo(out);
        }
        else {
            BatchEncoder.encode(out, batch, batchEdit, views, edits);
        }
    }

    record InternalEdit(boolean keep, boolean drop, FieldEdits fields) {}

    interface InternalRecordTransform {
        InternalEdit transform(BatchView batch, RecordView record);
    }

    interface AsyncInternalRecordTransform {
        CompletionStage<InternalEdit> transform(BatchView batch, RecordView record);
    }
}

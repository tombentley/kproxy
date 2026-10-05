/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.transform.records;

import java.nio.ByteBuffer;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executor;

import io.kroxylicious.kafka.common.record.internal.MemoryRecords;
import io.kroxylicious.kafka.common.record.internal.MutableRecordBatch;
import io.kroxylicious.kafka.common.utils.ByteBufferOutputStream;
import io.kroxylicious.transform.records.RecordFrames.AsyncInternalRecordTransform;
import io.kroxylicious.transform.records.RecordFrames.InternalEdit;

import edu.umd.cs.findbugs.annotations.Nullable;

/**
 * <p>The asynchronous, bounded-window driver behind {@link RecordFrames}' {@code apply*Async} terminals. It
 * runs a single pass over the batches: it pulls record views in order, hands each to the (async) record
 * transform, and keeps a bounded number of transforms outstanding, while emitting the encoded batches in
 * strict batch/offset order.</p>
 *
 * <h2>The window</h2>
 * <p>The window size {@code W} gates <em>reading ahead</em> to new batches: a new batch is opened only while
 * {@code issued - emitted < W}. Because a batch emits atomically (all its records at once), an opened batch is
 * always issued in full — otherwise a window smaller than a batch could never complete it (deadlock). So the
 * records in flight at once are bounded by {@code W} plus, at most, the size of the batch currently being
 * issued — exactly {@code W} for single-record batches (the {@code linger.ms=0} case this is designed for).
 * Because emission is in order, this also bounds the decoded records held in memory, independently of how the
 * client framed its batches: {@code W} concurrent lookups whether the records arrive in one batch or {@code W}
 * single-record batches. Capping only in-flight work would instead be unbounded — a slow <em>head</em> batch
 * with a fast tail would read the whole stream into memory behind it.</p>
 *
 * <h2>Threading</h2>
 * <p>All state is mutated only on the assembly thread ({@link TransformOptions#executor()}). The terminal must
 * be invoked on that thread, and transform completions are marshalled back onto it. A re-entrancy guard
 * ({@link #pumping}) makes a synchronously-completing transform (a warm cache, or {@code keep()}) safe: it
 * flows straight through without a thread hop and without permanently holding a window slot.</p>
 *
 * <h2>Errors</h2>
 * <p>Fail-only: any record-transform failure (or a synchronous error while opening a batch) completes the
 * returned stage exceptionally; later completions become no-ops.</p>
 */
final class AsyncTransformDriver {

    private final Iterator<MutableRecordBatch> batchIt;
    private final BatchTransform batchTransform;
    private final AsyncInternalRecordTransform transform;
    private final ByteBufferOutputStream out;
    private final int window;
    private final Executor executor;
    private final CompletableFuture<MemoryRecords> result = new CompletableFuture<>();

    /** Batch slots in batch order; emission is strictly from the head. */
    private final Deque<Slot> queue = new ArrayDeque<>();
    /** The batch currently being fed into the window (null between batches). */
    private @Nullable Slot openSlot;
    /** Total data records admitted (transform started). */
    private long issued;
    /** Total data records emitted. */
    private long emitted;

    /** Re-entrancy guard for {@link #pump()} (single assembly thread). */
    private boolean pumping;
    private boolean pumpAgain;

    AsyncTransformDriver(MemoryRecords records,
                         BatchTransform batchTransform,
                         AsyncInternalRecordTransform transform,
                         ByteBufferOutputStream out,
                         TransformOptions options) {
        this.batchIt = records.batches().iterator();
        this.batchTransform = Objects.requireNonNull(batchTransform);
        this.transform = Objects.requireNonNull(transform);
        this.out = Objects.requireNonNull(out);
        this.window = options.windowSize();
        this.executor = options.executor();
    }

    CompletionStage<MemoryRecords> run() {
        pump();
        return result;
    }

    private void pump() {
        if (pumping) {
            // Re-entered from an inline (warm) completion; let the active pump pick it up.
            pumpAgain = true;
            return;
        }
        pumping = true;
        try {
            do {
                pumpAgain = false;
                step();
            } while (pumpAgain);
        }
        finally {
            pumping = false;
        }
    }

    private void step() {
        try {
            boolean progress = true;
            while (progress && !result.isDone()) {
                progress = false;

                // Emit ready batches from the head, in order.
                while (!queue.isEmpty() && queue.peekFirst().ready()) {
                    Slot slot = queue.removeFirst();
                    emit(slot);
                    emitted += slot.recordCount();
                    progress = true;
                }

                // Admit records. A batch emits atomically (all its records at once), so once a batch is opened
                // it is issued in full — otherwise a window smaller than the batch could never complete it
                // (deadlock). The window therefore gates only *reading ahead* to a new batch: the next batch is
                // opened only while there is budget. In-flight is thus bounded by the window plus, at most, the
                // size of the batch currently being issued (exactly the window for single-record batches).
                while (true) {
                    if (openSlot != null && !openSlot.allIssued()) {
                        issueRecord(openSlot, openSlot.nextIndex++);
                        progress = true;
                        if (result.isDone()) {
                            return;
                        }
                        continue;
                    }
                    openSlot = null;
                    if (issued - emitted >= window || !batchIt.hasNext()) {
                        break;
                    }
                    openSlot = openNext();
                    progress = true;
                }
            }

            if (!result.isDone() && queue.isEmpty() && openSlot == null && !batchIt.hasNext()) {
                finish();
            }
        }
        catch (RuntimeException e) {
            fail(e);
        }
    }

    /**
     * Opens the next batch, appending its slot to the queue. Returns the slot if it is a data batch with
     * records to issue, or {@code null} for a control or empty batch (which is queued ready-to-emit and needs
     * no records issued).
     */
    private @Nullable Slot openNext() {
        MutableRecordBatch batch = batchIt.next();
        if (batch.isControlBatch()) {
            // Control batches are opaque and order-sensitive: copy verbatim, never transform.
            queue.addLast(Slot.control(batch));
            return null;
        }
        BatchView batchView = new BatchViewImpl(batch);
        BatchEdit batchEdit = batchTransform.transform(batchView);
        List<RecordView> views = new ArrayList<>();
        for (RecordView view : batchView.records()) {
            views.add(view);
        }
        Slot slot = Slot.data(batch, batchView, batchEdit, views);
        queue.addLast(slot);
        return views.isEmpty() ? null : slot;
    }

    private void issueRecord(Slot slot, int index) {
        BatchView batchView = Objects.requireNonNull(slot.batchView);
        CompletionStage<InternalEdit> stage = transform.transform(batchView, slot.views.get(index));
        issued++;
        marshal(stage).whenComplete((edit, error) -> onRecordDone(slot, index, edit, error));
    }

    /**
     * Marshals a transform's completion onto the assembly thread, unless it has already completed — in which
     * case it is left on the current (assembly) thread so the warm path pays no thread hop.
     */
    private CompletionStage<InternalEdit> marshal(CompletionStage<InternalEdit> stage) {
        CompletableFuture<InternalEdit> future = stage.toCompletableFuture();
        if (future.isDone()) {
            return stage;
        }
        return future.whenCompleteAsync((edit, error) -> {
        }, executor);
    }

    private void onRecordDone(Slot slot, int index, @Nullable InternalEdit edit, @Nullable Throwable error) {
        if (result.isDone()) {
            return;
        }
        if (error != null) {
            fail(error);
            return;
        }
        slot.edits[index] = edit;
        slot.resolved++;
        pump();
    }

    private void emit(Slot slot) {
        if (slot.control) {
            slot.batch.writeTo(out);
        }
        else {
            RecordFrames.encodeBatch(out, slot.batch, Objects.requireNonNull(slot.batchEdit), slot.views, Arrays.asList(slot.edits));
        }
    }

    private void finish() {
        ByteBuffer buffer = out.buffer();
        buffer.flip();
        result.complete(MemoryRecords.readableRecords(buffer));
    }

    private void fail(Throwable error) {
        result.completeExceptionally(unwrap(error));
    }

    private static Throwable unwrap(Throwable error) {
        if (error instanceof CompletionException && error.getCause() != null) {
            return error.getCause();
        }
        return error;
    }

    /**
     * A batch awaiting its records' edits before it can be encoded. A control (or empty) slot carries no
     * records and is ready to emit immediately; a data slot is ready once every record's edit has resolved.
     */
    private static final class Slot {

        private final MutableRecordBatch batch;
        private final boolean control;
        private final @Nullable BatchView batchView;
        private final @Nullable BatchEdit batchEdit;
        private final List<RecordView> views;
        private final InternalEdit[] edits;
        private int nextIndex;
        private int resolved;

        private Slot(MutableRecordBatch batch, boolean control, @Nullable BatchView batchView,
                     @Nullable BatchEdit batchEdit, List<RecordView> views) {
            this.batch = batch;
            this.control = control;
            this.batchView = batchView;
            this.batchEdit = batchEdit;
            this.views = views;
            this.edits = new InternalEdit[views.size()];
        }

        static Slot control(MutableRecordBatch batch) {
            return new Slot(batch, true, null, null, List.of());
        }

        static Slot data(MutableRecordBatch batch, BatchView batchView, BatchEdit batchEdit, List<RecordView> views) {
            return new Slot(batch, false, batchView, batchEdit, views);
        }

        int recordCount() {
            return views.size();
        }

        boolean allIssued() {
            return nextIndex >= views.size();
        }

        boolean ready() {
            return control || resolved == views.size();
        }
    }
}

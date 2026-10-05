/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.transform.records;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executor;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import io.kroxylicious.kafka.common.compress.Compression;
import io.kroxylicious.kafka.common.record.internal.MemoryRecords;
import io.kroxylicious.kafka.common.record.internal.MutableRecordBatch;
import io.kroxylicious.kafka.common.record.internal.Record;
import io.kroxylicious.kafka.common.record.internal.SimpleRecord;
import io.kroxylicious.kafka.common.utils.ByteBufferOutputStream;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests for the asynchronous, bounded-window terminals of {@link RecordFrames}, focused on the driver's
 * concerns: output is byte-identical to the synchronous path for the same edits; batches are emitted in order
 * even when transforms complete out of order; the window {@code W} bounds concurrency across batch boundaries
 * (the property per-batch windowing could not provide); a slow head batch caps read-ahead and blocks emission;
 * the warm/{@code keep()} path streams through without holding a window slot; and any failure fails the whole
 * stage (fail-only).
 *
 * <p>The harness is deterministic: a same-thread executor runs marshalled completions inline, and a
 * controllable transform hands back futures the test completes explicitly — no real threads, no sleeps.</p>
 */
class RecordFramesAsyncTest {

    private static final Executor DIRECT = Runnable::run;

    static Stream<Compression> compressions() {
        return Stream.of(
                Compression.NONE,
                Compression.gzip().build(),
                Compression.snappy().build(),
                Compression.lz4().build(),
                Compression.zstd().build());
    }

    @ParameterizedTest
    @MethodSource("compressions")
    void asyncValueEditIsByteIdenticalToSyncPath(Compression compression) {
        // Given
        MemoryRecords input = MemoryRecords.withRecords(5L, compression,
                new SimpleRecord(100L, "k0".getBytes(UTF_8), "v0".getBytes(UTF_8)),
                new SimpleRecord(101L, "k1".getBytes(UTF_8), "v1".getBytes(UTF_8)));
        MemoryRecords expected = ProducerRecords.of(input).applyProduce(
                (batch, record) -> ProducerRecordEdit.value(bb("V:" + str(record.value()))), newBuffer());

        // When
        CompletionStage<MemoryRecords> stage = ProducerRecords.of(input).applyProduceAsync(
                (batch, record) -> CompletableFuture.completedFuture(ProducerRecordEdit.value(bb("V:" + str(record.value())))),
                newBuffer(), options(4));

        // Then
        MemoryRecords actual = completed(stage);
        assertThat(bytes(actual)).isEqualTo(bytes(expected));
        assertAllValid(actual);
    }

    @Test
    void asyncFetchDropIsByteIdenticalToSyncPath() {
        // Given
        MemoryRecords input = MemoryRecords.withRecords(0L, Compression.NONE,
                new SimpleRecord(100L, "k0".getBytes(UTF_8), "v0".getBytes(UTF_8)),
                new SimpleRecord(101L, "k1".getBytes(UTF_8), "v1".getBytes(UTF_8)),
                new SimpleRecord(102L, "k2".getBytes(UTF_8), "v2".getBytes(UTF_8)));
        MemoryRecords expected = ConsumerRecords.of(input).applyFetch(
                (batch, record) -> record.offset() == 1L ? ConsumerRecordEdit.drop() : ConsumerRecordEdit.keep(), newBuffer());

        // When
        CompletionStage<MemoryRecords> stage = ConsumerRecords.of(input).applyFetchAsync(
                (batch, record) -> CompletableFuture.completedFuture(
                        record.offset() == 1L ? ConsumerRecordEdit.drop() : ConsumerRecordEdit.keep()),
                newBuffer(), options(4));

        // Then
        MemoryRecords actual = completed(stage);
        assertThat(bytes(actual)).isEqualTo(bytes(expected));
        assertAllValid(actual);
    }

    @Test
    void emitsBatchesInOrderDespiteScrambledCompletion() {
        // Given
        MemoryRecords input = singleRecordBatches(4);
        ControllableTransform transform = new ControllableTransform();

        // When
        CompletionStage<MemoryRecords> stage = ProducerRecords.of(input).applyProduceAsync(transform, newBuffer(), options(10));
        transform.complete("v2", ProducerRecordEdit.keep());
        transform.complete("v0", ProducerRecordEdit.keep());
        transform.complete("v3", ProducerRecordEdit.keep());
        transform.complete("v1", ProducerRecordEdit.keep());

        // Then
        MemoryRecords actual = completed(stage);
        assertThat(records(actual)).extracting(Record::offset).containsExactly(0L, 1L, 2L, 3L);
        assertAllValid(actual);
    }

    @Test
    void windowBoundsConcurrentTransformsAcrossSingleRecordBatches() {
        // Given
        MemoryRecords input = singleRecordBatches(8);
        ControllableTransform transform = new ControllableTransform();

        // When
        CompletionStage<MemoryRecords> stage = ProducerRecords.of(input).applyProduceAsync(transform, newBuffer(), options(4));

        // Then
        assertThat(transform.invocations).isEqualTo(4);
        assertThat(stage.toCompletableFuture()).isNotDone();
        for (int i = 0; i < 8; i++) {
            transform.complete("v" + i, ProducerRecordEdit.keep());
        }
        MemoryRecords actual = completed(stage);
        assertThat(transform.peakOutstanding).isEqualTo(4);
        assertThat(records(actual)).extracting(Record::offset).containsExactly(0L, 1L, 2L, 3L, 4L, 5L, 6L, 7L);
    }

    @Test
    void pendingHeadCapsReadAheadAndBlocksEmission() {
        // Given
        MemoryRecords input = singleRecordBatches(6);
        ControllableTransform transform = new ControllableTransform();

        // When
        CompletionStage<MemoryRecords> stage = ProducerRecords.of(input).applyProduceAsync(transform, newBuffer(), options(3));
        transform.complete("v1", ProducerRecordEdit.keep());
        transform.complete("v2", ProducerRecordEdit.keep());

        // Then
        assertThat(transform.invocations).isEqualTo(3);
        assertThat(stage.toCompletableFuture()).isNotDone();
        transform.complete("v0", ProducerRecordEdit.keep());
        transform.complete("v3", ProducerRecordEdit.keep());
        transform.complete("v4", ProducerRecordEdit.keep());
        transform.complete("v5", ProducerRecordEdit.keep());
        MemoryRecords actual = completed(stage);
        assertThat(records(actual)).extracting(Record::offset).containsExactly(0L, 1L, 2L, 3L, 4L, 5L);
    }

    @ParameterizedTest
    @MethodSource("compressions")
    void warmKeepStreamsThroughWithWindowSmallerThanBatch(Compression compression) {
        // Given
        MemoryRecords input = MemoryRecords.withRecords(0L, compression,
                new SimpleRecord(1L, "k0".getBytes(UTF_8), "v0".getBytes(UTF_8)),
                new SimpleRecord(2L, "k1".getBytes(UTF_8), "v1".getBytes(UTF_8)),
                new SimpleRecord(3L, "k2".getBytes(UTF_8), "v2".getBytes(UTF_8)));

        // When
        CompletionStage<MemoryRecords> stage = ProducerRecords.of(input).applyProduceAsync(
                (batch, record) -> CompletableFuture.completedFuture(ProducerRecordEdit.keep()), newBuffer(), options(1));

        // Then
        MemoryRecords actual = completed(stage);
        assertThat(bytes(actual)).isEqualTo(bytes(input));
    }

    @Test
    void recordFailureFailsTheStage() {
        // Given
        MemoryRecords input = singleRecordBatches(3);
        ControllableTransform transform = new ControllableTransform();

        // When
        CompletionStage<MemoryRecords> stage = ProducerRecords.of(input).applyProduceAsync(transform, newBuffer(), options(4));
        transform.fail("v1", new RuntimeException("boom"));

        // Then
        assertThat(stage.toCompletableFuture()).isCompletedExceptionally();
        assertThatThrownBy(() -> stage.toCompletableFuture().join()).hasRootCauseMessage("boom");
    }

    @Test
    void synchronouslyThrowingTransformFailsTheStage() {
        // Given
        MemoryRecords input = singleRecordBatches(2);

        // When
        CompletionStage<MemoryRecords> stage = ProducerRecords.of(input).applyProduceAsync(
                (batch, record) -> {
                    throw new IllegalStateException("sync boom");
                }, newBuffer(), options(4));

        // Then
        assertThat(stage.toCompletableFuture()).isCompletedExceptionally();
        assertThatThrownBy(() -> stage.toCompletableFuture().join()).hasRootCauseMessage("sync boom");
    }

    // --- harness ---

    /**
     * An {@link AsyncProducerRecordTransform} that hands back a pending future per record (keyed by the
     * record's value), which the test completes explicitly. Tracks how many transforms have been invoked and
     * the peak number outstanding (invoked minus completed) to assert the window bound.
     */
    private static final class ControllableTransform implements AsyncProducerRecordTransform {

        private final Map<String, CompletableFuture<ProducerRecordEdit>> byValue = new LinkedHashMap<>();
        private int invocations;
        private int completedCount;
        private int peakOutstanding;

        @Override
        public CompletionStage<ProducerRecordEdit> transform(BatchView batch, RecordView record) {
            invocations++;
            peakOutstanding = Math.max(peakOutstanding, invocations - completedCount);
            CompletableFuture<ProducerRecordEdit> future = new CompletableFuture<>();
            byValue.put(str(record.value()), future);
            return future;
        }

        void complete(String value, ProducerRecordEdit edit) {
            // Count the completion before firing it: completing a future can synchronously drive the next
            // admission, and the outstanding count must reflect that this one is no longer in flight.
            completedCount++;
            future(value).complete(edit);
        }

        void fail(String value, Throwable error) {
            completedCount++;
            future(value).completeExceptionally(error);
        }

        private CompletableFuture<ProducerRecordEdit> future(String value) {
            CompletableFuture<ProducerRecordEdit> future = byValue.get(value);
            assertThat(future).as("no pending transform for value %s", value).isNotNull();
            return future;
        }
    }

    // --- helpers ---

    private static TransformOptions options(int windowSize) {
        return TransformOptions.builder().windowSize(windowSize).executor(DIRECT).build();
    }

    private static ByteBufferOutputStream newBuffer() {
        return new ByteBufferOutputStream(1024);
    }

    private static ByteBuffer bb(String s) {
        return ByteBuffer.wrap(s.getBytes(UTF_8));
    }

    private static String str(ByteBuffer buffer) {
        return StandardCharsets.UTF_8.decode(buffer.duplicate()).toString();
    }

    private static MemoryRecords completed(CompletionStage<MemoryRecords> stage) {
        CompletableFuture<MemoryRecords> future = stage.toCompletableFuture();
        assertThat(future).isCompleted();
        return future.join();
    }

    private static byte[] bytes(MemoryRecords records) {
        ByteBuffer buffer = records.buffer().duplicate();
        byte[] out = new byte[buffer.remaining()];
        buffer.get(out);
        return out;
    }

    private static List<Record> records(MemoryRecords records) {
        List<Record> out = new ArrayList<>();
        for (MutableRecordBatch batch : records.batches()) {
            for (Record record : batch) {
                out.add(record);
            }
        }
        return out;
    }

    private static void assertAllValid(MemoryRecords records) {
        for (MutableRecordBatch batch : records.batches()) {
            assertThat(batch.isValid()).isTrue();
        }
    }

    /** {@code n} single-record batches, base offsets {@code 0..n-1}, values {@code v0..v(n-1)}. */
    private static MemoryRecords singleRecordBatches(int n) {
        MemoryRecords acc = null;
        for (int i = 0; i < n; i++) {
            MemoryRecords batch = MemoryRecords.withRecords(i, Compression.NONE,
                    new SimpleRecord(100L + i, ("k" + i).getBytes(UTF_8), ("v" + i).getBytes(UTF_8)));
            acc = acc == null ? batch : concat(acc, batch);
        }
        return acc;
    }

    private static MemoryRecords concat(MemoryRecords first, MemoryRecords second) {
        ByteBuffer a = first.buffer();
        ByteBuffer b = second.buffer();
        ByteBuffer out = ByteBuffer.allocate(a.remaining() + b.remaining());
        out.put(a.duplicate());
        out.put(b.duplicate());
        out.flip();
        return MemoryRecords.readableRecords(out);
    }
}

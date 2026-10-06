/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.record.manipulation.filter.resolve;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executor;
import java.util.function.IntFunction;

import io.kroxylicious.kafka.common.record.internal.MemoryRecords;
import io.kroxylicious.kafka.common.utils.ByteBufferOutputStream;
import io.kroxylicious.transform.records.ConsumerRecords;
import io.kroxylicious.transform.records.ProducerRecords;
import io.kroxylicious.transform.records.TransformOptions;

/**
 * Applies the configured manipulations to each partition's records using the {@code records} transform engine.
 *
 * <p>Each partition is transformed independently via {@link ProducerRecords}/{@link ConsumerRecords}; the
 * engine's bounded-window driver assembles output on the supplied {@link Executor} (the filter dispatch
 * executor) and copies unmatched records verbatim. The async terminals must be invoked on that executor, so
 * each partition's work is dispatched onto it; completions are then marshalled back onto it too.</p>
 */
public final class ManipulationApplier {

    /** Bounds the records admitted-but-not-yet-emitted per partition (concurrency, external-service load, heap). */
    private static final int DEFAULT_WINDOW_SIZE = 64;

    private final Executor executor;
    private final int windowSize;

    public ManipulationApplier(Executor executor) {
        this(executor, DEFAULT_WINDOW_SIZE);
    }

    public ManipulationApplier(Executor executor, int windowSize) {
        this.executor = executor;
        this.windowSize = windowSize;
    }

    /**
     * Transforms each produce-path work item, writing the transformed records back.
     * @param workItems the partitions to transform.
     * @param bufferFactory allocates an output buffer of at least the given capacity.
     * @return a stage completing when every partition has been transformed and written back.
     */
    public CompletionStage<Void> applyProduce(List<PartitionWorkItem> workItems,
                                              IntFunction<ByteBufferOutputStream> bufferFactory) {
        return apply(workItems, bufferFactory, (workItem, out, options) -> ProducerRecords.of(workItem.records())
                .applyProduceAsync(new ManipulationProducerTransform(workItem.manipulations()), out, options));
    }

    /**
     * Transforms each consume-path (fetch / share-fetch) work item, writing the transformed records back.
     * @param workItems the partitions to transform.
     * @param bufferFactory allocates an output buffer of at least the given capacity.
     * @return a stage completing when every partition has been transformed and written back.
     */
    public CompletionStage<Void> applyFetch(List<PartitionWorkItem> workItems,
                                            IntFunction<ByteBufferOutputStream> bufferFactory) {
        return apply(workItems, bufferFactory, (workItem, out, options) -> ConsumerRecords.of(workItem.records())
                .applyFetchAsync(new ManipulationConsumerTransform(workItem.manipulations()), out, options));
    }

    private CompletionStage<Void> apply(List<PartitionWorkItem> workItems,
                                        IntFunction<ByteBufferOutputStream> bufferFactory,
                                        Terminal terminal) {
        TransformOptions options = TransformOptions.builder().executor(executor).windowSize(windowSize).build();
        List<CompletableFuture<Void>> partitions = new ArrayList<>(workItems.size());
        for (PartitionWorkItem workItem : workItems) {
            ByteBufferOutputStream out = bufferFactory.apply(Math.max(workItem.records().sizeInBytes(), 1));
            CompletableFuture<Void> partition = CompletableFuture.<Void> completedFuture(null)
                    // the async terminal must be invoked on the assembly executor
                    .thenComposeAsync(ignored -> terminal.apply(workItem, out, options), executor)
                    .thenAccept(workItem.writeBack())
                    .toCompletableFuture();
            partitions.add(partition);
        }
        return CompletableFuture.allOf(partitions.toArray(new CompletableFuture[0]));
    }

    @FunctionalInterface
    private interface Terminal {
        CompletionStage<MemoryRecords> apply(PartitionWorkItem workItem, ByteBufferOutputStream out, TransformOptions options);
    }
}

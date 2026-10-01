/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.record.manipulation.filter.resolve;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executor;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.function.Predicate;

import io.kroxylicious.filter.record.manipulation.filter.SchemaRegistryClient.VersionsResult;
import io.kroxylicious.filter.record.manipulation.filter.config.HeaderPredicate;
import io.kroxylicious.kafka.common.header.Header;
import io.kroxylicious.kafka.common.record.internal.MemoryRecords;
import io.kroxylicious.kafka.common.record.internal.Record;
import io.kroxylicious.kafka.common.utils.ByteBufferOutputStream;
import io.kroxylicious.kafka.transform.RecordStream;

import edu.umd.cs.findbugs.annotations.Nullable;

/**
 * Coordinates, for a whole request/response, the gather &rarr; resolve &rarr; match &rarr; transform
 * pipeline across all its partitions.
 *
 * <p>Lookup ids are pooled across every {@link PartitionWorkItem} and resolved together, so partitions
 * that share a schema resolve it once. To avoid re-decompressing compressed batches more than
 * necessary, there is a single gather pass (which also stores the small per-record state needed for
 * matching), matching and content-key derivation happen in memory, and a second (transform) pass runs
 * only for partitions that actually matched — partitions with no matches keep their original records.</p>
 */
public final class ManipulationOrchestrator {

    /** Sentinel for "this record has no id of this kind". */
    private static final long ABSENT = Long.MIN_VALUE;
    /** The per-record synchronous-match bitmask is a {@code long}, so at most 64 manipulations per topic. */
    private static final int MAX_MANIPULATIONS = Long.SIZE;

    @Nullable
    private final SchemaFacets schemaFacets;
    private final Executor filterExecutor;

    /**
     * Creates an orchestrator that resolves via {@code schemaFacets} and completes on {@code filterExecutor}.
     * @param schemaFacets the schema resolvers, or {@code null} if no manipulation uses schema selection.
     * @param filterExecutor the filter's single-threaded dispatch executor; resolution continuations are
     *                       hopped onto it so filter state is mutated only on the filter thread.
     */
    public ManipulationOrchestrator(@Nullable SchemaFacets schemaFacets, Executor filterExecutor) {
        this.schemaFacets = schemaFacets;
        this.filterExecutor = filterExecutor;
    }

    /**
     * Process the given work items, replacing each matched partition's records via its
     * {@link PartitionWorkItem#writeBack()}.
     *
     * @param workItems the partitions to process (each with its applicable manipulations).
     * @param bufferFactory allocates an output buffer of at least the given capacity (e.g.
     *                      {@code context::createByteBufferOutputStream}).
     * @return a stage completing when every matched partition's records have been written back.
     */
    public CompletionStage<Void> process(List<PartitionWorkItem> workItems, IntFunction<ByteBufferOutputStream> bufferFactory) {
        if (workItems.isEmpty()) {
            return CompletableFuture.<Void> completedFuture(null);
        }
        return processNonEmpty(workItems, bufferFactory);
    }

    private CompletionStage<Void> processNonEmpty(List<PartitionWorkItem> workItems, IntFunction<ByteBufferOutputStream> bufferFactory) {
        workItems.forEach(wi -> checkManipulationCount(wi.manipulations()));

        boolean needKeySchema = schemaFacets != null && anyManipulation(workItems, CompiledManipulation::needsKeySchema);
        boolean needValueSchema = schemaFacets != null && anyManipulation(workItems, CompiledManipulation::needsValueSchema);
        boolean needSchema = needKeySchema || needValueSchema;
        boolean needSync = anyManipulation(workItems, m -> m.headerPredicate() != null);

        // Fast path: selection is purely by topic/direction, so every record matches its first
        // applicable manipulation. No gather pass, no resolution.
        if (!needSchema && !needSync) {
            ResolvedEntities empty = ResolvedEntities.empty();
            for (PartitionWorkItem wi : workItems) {
                CompiledManipulation first = wi.manipulations().get(0);
                MemoryRecords out = RecordStream.ofRecords(wi.records())
                        .mapConstant(first)
                        .toMemoryRecords(newBuffer(bufferFactory, wi.records()), new ManipulationRecordTransform(empty));
                wi.writeBack().accept(out);
            }
            return CompletableFuture.<Void> completedFuture(null);
        }

        // Single gather pass per work item: collect the union of ids and the per-record match state.
        Set<Long> allGlobalIds = new HashSet<>();
        long[][][] slotsByWorkItem = new long[workItems.size()][][];
        for (int w = 0; w < workItems.size(); w++) {
            PartitionWorkItem wi = workItems.get(w);
            List<CompiledManipulation> manipulations = wi.manipulations();
            List<long[]> slots = new ArrayList<>();
            RecordStream.ofRecords(wi.records()).forEachRecord((batch, record, ignored) -> {
                long keyId = needKeySchema ? extract(schemaFacets.keyExtractor(), record) : ABSENT;
                long valueId = needValueSchema ? extract(schemaFacets.valueExtractor(), record) : ABSENT;
                if (keyId != ABSENT) {
                    allGlobalIds.add(keyId);
                }
                if (valueId != ABSENT) {
                    allGlobalIds.add(valueId);
                }
                long syncBits = 0L;
                for (int i = 0; i < manipulations.size(); i++) {
                    var predicate = manipulations.get(i).headerPredicate();
                    if (predicate == null || predicate.test(record.headers())) {
                        syncBits |= (1L << i);
                    }
                }
                slots.add(new long[]{ keyId, valueId, syncBits });
            });
            slotsByWorkItem[w] = slots.toArray(new long[0][]);
        }

        // Resolve identities once (deduped + cached), on the filter thread.
        CompletionStage<Map<Long, VersionsResult>> versionsStage = needSchema
                ? schemaFacets.versionResolver().resolveAll(allGlobalIds)
                : CompletableFuture.<Map<Long, VersionsResult>> completedFuture(Map.of());

        return hop(versionsStage).thenCompose(versions -> {
            // Match in memory and derive the content ids needed by the matched records' `then`.
            Set<Long> contentIds = new HashSet<>();
            int[][] matchByWorkItem = new int[workItems.size()][];
            for (int w = 0; w < workItems.size(); w++) {
                PartitionWorkItem wi = workItems.get(w);
                long[][] slots = slotsByWorkItem[w];
                List<CompiledManipulation> manipulations = wi.manipulations();
                int[] matchIndex = new int[slots.length];
                for (int pos = 0; pos < slots.length; pos++) {
                    long[] slot = slots[pos];
                    VersionsResult keyVersions = slot[0] == ABSENT ? null : versions.get(slot[0]);
                    VersionsResult valueVersions = slot[1] == ABSENT ? null : versions.get(slot[1]);
                    int matched = firstMatch(manipulations, slot[2], keyVersions, valueVersions);
                    matchIndex[pos] = matched;
                    if (matched >= 0) {
                        CompiledManipulation manipulation = manipulations.get(matched);
                        if (keyVersions != null && manipulation.needsKeySchema()) {
                            contentIds.add(keyVersions.contentId());
                        }
                        if (valueVersions != null && manipulation.needsValueSchema()) {
                            contentIds.add(valueVersions.contentId());
                        }
                    }
                }
                matchByWorkItem[w] = matchIndex;
            }

            // Resolve content once (deduped + cached), on the filter thread.
            CompletionStage<Map<Long, byte[]>> contentStage = (needSchema && !contentIds.isEmpty())
                    ? schemaFacets.contentResolver().resolveAll(contentIds)
                    : CompletableFuture.<Map<Long, byte[]>> completedFuture(Map.of());

            return hop(contentStage).thenAccept(content -> {
                ResolvedEntities resolved = ResolvedEntities.builder()
                        .put(SchemaFacets.SCHEMA_VERSIONS, versions)
                        .put(SchemaFacets.SCHEMA_CONTENT, content)
                        .build();
                // Transform matched partitions only; unmatched partitions keep their original records.
                for (int w = 0; w < workItems.size(); w++) {
                    PartitionWorkItem wi = workItems.get(w);
                    int[] matchIndex = matchByWorkItem[w];
                    if (anyMatched(matchIndex)) {
                        List<CompiledManipulation> manipulations = wi.manipulations();
                        MemoryRecords out = RecordStream.ofRecordsWithIndex(wi.records())
                                .mapPerRecord((batch, record, index) -> {
                                    int matched = matchIndex[index];
                                    return matched < 0 ? null : manipulations.get(matched);
                                })
                                .toMemoryRecords(newBuffer(bufferFactory, wi.records()), new ManipulationRecordTransform(resolved));
                        wi.writeBack().accept(out);
                    }
                }
            });
        });
    }

    private <T> CompletionStage<T> hop(CompletionStage<T> stage) {
        return stage.thenApplyAsync(Function.identity(), filterExecutor);
    }

    private static ByteBufferOutputStream newBuffer(IntFunction<ByteBufferOutputStream> bufferFactory, MemoryRecords records) {
        return bufferFactory.apply(Math.max(records.sizeInBytes(), 1));
    }

    private static long extract(LookupKeyExtractor<Long> extractor, Record record) {
        Long id = extractor.extract(record);
        return id == null ? ABSENT : id;
    }

    private static int firstMatch(List<CompiledManipulation> manipulations,
                                  long syncBits,
                                  @Nullable VersionsResult keyVersions,
                                  @Nullable VersionsResult valueVersions) {
        for (int i = 0; i < manipulations.size(); i++) {
            if ((syncBits & (1L << i)) != 0 && manipulations.get(i).matchesSchema(keyVersions, valueVersions)) {
                return i;
            }
        }
        return -1;
    }

    private static boolean headerMatches(Record record, HeaderPredicate predicate) {
        for (Header header : record.headers()) {
            String key = header.key();
            if ((predicate.withKey() != null && predicate.withKey().equals(key))
                    || (predicate.withKeyIn() != null && predicate.withKeyIn().contains(key))
                    || (predicate.withKeyMatching() != null && key.matches(predicate.withKeyMatching()))) {
                return true;
            }
        }
        return false;
    }

    private static boolean anyMatched(int[] matchIndex) {
        for (int matched : matchIndex) {
            if (matched >= 0) {
                return true;
            }
        }
        return false;
    }

    private static boolean anyManipulation(List<PartitionWorkItem> workItems, Predicate<CompiledManipulation> predicate) {
        return workItems.stream().flatMap(wi -> wi.manipulations().stream()).anyMatch(predicate);
    }

    private static void checkManipulationCount(List<CompiledManipulation> manipulations) {
        if (manipulations.size() > MAX_MANIPULATIONS) {
            throw new IllegalArgumentException(
                    "A topic may have at most " + MAX_MANIPULATIONS + " manipulations, but was configured with " + manipulations.size());
        }
    }
}

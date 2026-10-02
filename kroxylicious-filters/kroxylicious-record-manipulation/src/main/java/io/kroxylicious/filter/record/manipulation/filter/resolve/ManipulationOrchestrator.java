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

import io.kroxylicious.filter.record.manipulation.filter.ContentId;
import io.kroxylicious.filter.record.manipulation.filter.GlobalId;
import io.kroxylicious.filter.record.manipulation.filter.SchemaRegistryClient.VersionsResult;
import io.kroxylicious.filter.record.manipulation.filter.apicurio.SchemaFacets;
import io.kroxylicious.kafka.common.record.internal.MemoryRecords;
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

    /** The per-record synchronous-match bitmask is a {@code long}, so at most 64 manipulations per topic. */
    private static final int MAX_MANIPULATIONS = Long.SIZE;
    private static final int NO_MATCH = -1;

    @Nullable
    private final SchemaFacets schemaFacets;
    private final Executor filterExecutor;

    /**
     * Creates an orchestrator that resolves via {@code schemaFacets} and completes on {@code filterExecutor}.
     * @param schemaFacets the schema resolvers, or {@code null} if no manipulation uses schema selection.
     * @param filterExecutor the filter's single-threaded dispatch executor; resolution continuations are
     *                       hopped onto it so filter state is mutated only on the filter thread.
     */
    public ManipulationOrchestrator(@Nullable SchemaFacets schemaFacets,
                                    Executor filterExecutor) {
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
    public CompletionStage<Void> process(List<PartitionWorkItem> workItems,
                                         IntFunction<ByteBufferOutputStream> bufferFactory) {
        if (workItems.isEmpty()) {
            return CompletableFuture.<Void> completedFuture(null);
        }
        return processNonEmpty(workItems, bufferFactory);
    }

    private CompletionStage<Void> processNonEmpty(List<PartitionWorkItem> workItems,
                                                  IntFunction<ByteBufferOutputStream> bufferFactory) {
        workItems.forEach(wi -> checkManipulationCount(wi.manipulations()));

        boolean needKeySchema = schemaFacets != null && anyManipulation(workItems, CompiledManipulation::needsKeySchema);
        boolean needValueSchema = schemaFacets != null && anyManipulation(workItems, CompiledManipulation::needsValueSchema);
        boolean needSchema = needKeySchema || needValueSchema;
        boolean needSync = anyManipulation(workItems, m -> m.headerPredicate() != null);

        if (!needSchema && !needSync) {
            return fastPath(workItems, bufferFactory);
        }

        GatherResult gathered = gather(workItems, needKeySchema, needValueSchema);

        CompletionStage<Map<GlobalId, VersionsResult>> versionsStage = needSchema
                ? schemaFacets.versionResolver().resolveAll(gathered.allGlobalIds())
                : CompletableFuture.completedFuture(Map.of());

        return hop(versionsStage).thenCompose(versions -> {
            MatchResult matched = match(workItems, gathered, versions);

            CompletionStage<Map<ContentId, byte[]>> contentStage = (needSchema && !matched.contentIds().isEmpty())
                    ? schemaFacets.contentResolver().resolveAll(matched.contentIds())
                    : CompletableFuture.completedFuture(Map.of());

            return hop(contentStage).thenAccept(content -> {
                ResolvedEntities resolved = ResolvedEntities.builder()
                        .put(SchemaFacets.VERSIONS_DEPENDENCY, versions)
                        .put(SchemaFacets.SCHEMA_CONTENT_DEPENDENCY, content)
                        .build();
                transformMatched(workItems, matched, resolved, bufferFactory);
            });
        });
    }

    private CompletionStage<Void> fastPath(List<PartitionWorkItem> workItems,
                                           IntFunction<ByteBufferOutputStream> bufferFactory) {
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

    private GatherResult gather(List<PartitionWorkItem> workItems,
                                boolean needKeySchema,
                                boolean needValueSchema) {
        Set<GlobalId> allGlobalIds = new HashSet<>();
        List<GatheredRecord[]> byWorkItem = new ArrayList<>();
        for (PartitionWorkItem wi : workItems) {
            List<CompiledManipulation> manipulations = wi.manipulations();
            List<GatheredRecord> records = new ArrayList<>();
            RecordStream.ofRecords(wi.records()).forEachRecord((batch, record, ignored) -> {
                @Nullable
                GlobalId keyId = needKeySchema ? schemaFacets.keyExtractor().extract(record) : null;
                @Nullable
                GlobalId valueId = needValueSchema ? schemaFacets.valueExtractor().extract(record) : null;
                if (keyId != null) {
                    allGlobalIds.add(keyId);
                }
                if (valueId != null) {
                    allGlobalIds.add(valueId);
                }
                long headerMatchMask = 0L;
                for (int i = 0; i < manipulations.size(); i++) {
                    var predicate = manipulations.get(i).headerPredicate();
                    if (predicate == null || predicate.test(record.headers())) {
                        headerMatchMask |= (1L << i);
                    }
                }
                records.add(new GatheredRecord(keyId, valueId, headerMatchMask));
            });
            byWorkItem.add(records.toArray(new GatheredRecord[0]));
        }
        return new GatherResult(allGlobalIds, byWorkItem);
    }

    private MatchResult match(List<PartitionWorkItem> workItems,
                              GatherResult gathered,
                              Map<GlobalId, VersionsResult> versions) {
        Set<ContentId> contentIds = new HashSet<>();
        List<int[]> matchByWorkItem = new ArrayList<>();
        for (int w = 0; w < workItems.size(); w++) {
            PartitionWorkItem wi = workItems.get(w);
            GatheredRecord[] gatheredRecords = gathered.byWorkItem().get(w);
            List<CompiledManipulation> manipulations = wi.manipulations();
            int[] matchIndex = new int[gatheredRecords.length];
            for (int pos = 0; pos < gatheredRecords.length; pos++) {
                GatheredRecord gatheredRecord = gatheredRecords[pos];
                VersionsResult keyVersions = gatheredRecord.keySchemaId() == null ? null : versions.get(gatheredRecord.keySchemaId());
                VersionsResult valueVersions = gatheredRecord.valueSchemaId() == null ? null : versions.get(gatheredRecord.valueSchemaId());
                int matched = firstMatch(manipulations, gatheredRecord.headerMatchMask(), keyVersions, valueVersions);
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
            matchByWorkItem.add(matchIndex);
        }
        return new MatchResult(matchByWorkItem, contentIds);
    }

    private void transformMatched(List<PartitionWorkItem> workItems,
                                  MatchResult matched,
                                  ResolvedEntities resolved,
                                  IntFunction<ByteBufferOutputStream> bufferFactory) {
        for (int w = 0; w < workItems.size(); w++) {
            PartitionWorkItem wi = workItems.get(w);
            int[] matchIndex = matched.byWorkItem().get(w);
            if (anyMatched(matchIndex)) {
                List<CompiledManipulation> manipulations = wi.manipulations();
                MemoryRecords out = RecordStream.ofRecordsWithIndex(wi.records())
                        .mapPerRecord((batch, record, index) -> {
                            int m = matchIndex[index];
                            return m == NO_MATCH ? null : manipulations.get(m);
                        })
                        .toMemoryRecords(newBuffer(bufferFactory, wi.records()), new ManipulationRecordTransform(resolved));
                wi.writeBack().accept(out);
            }
        }
    }

    private <T> CompletionStage<T> hop(CompletionStage<T> stage) {
        return stage.thenApplyAsync(Function.identity(), filterExecutor);
    }

    private static ByteBufferOutputStream newBuffer(IntFunction<ByteBufferOutputStream> bufferFactory,
                                                    MemoryRecords records) {
        return bufferFactory.apply(Math.max(records.sizeInBytes(), 1));
    }

    private static int firstMatch(List<CompiledManipulation> manipulations,
                                  long headerMatchMask,
                                  @Nullable VersionsResult keyVersions,
                                  @Nullable VersionsResult valueVersions) {
        for (int i = 0; i < manipulations.size(); i++) {
            if ((headerMatchMask & (1L << i)) != 0 && manipulations.get(i).matchesSchema(keyVersions, valueVersions)) {
                return i;
            }
        }
        return NO_MATCH;
    }

    private static boolean anyMatched(int[] matchIndex) {
        for (int matched : matchIndex) {
            if (matched >= 0) {
                return true;
            }
        }
        return false;
    }

    private static boolean anyManipulation(List<PartitionWorkItem> workItems,
                                           Predicate<CompiledManipulation> predicate) {
        return workItems.stream().flatMap(wi -> wi.manipulations().stream()).anyMatch(predicate);
    }

    private static void checkManipulationCount(List<CompiledManipulation> manipulations) {
        if (manipulations.size() > MAX_MANIPULATIONS) {
            throw new IllegalArgumentException(
                    "A topic may have at most " + MAX_MANIPULATIONS + " manipulations, but was configured with " + manipulations.size());
        }
    }

    private record GatherResult(
                                Set<GlobalId> allGlobalIds,
                                List<GatheredRecord[]> byWorkItem) {}

    private record GatheredRecord(
                                  @Nullable GlobalId keySchemaId,
                                  @Nullable GlobalId valueSchemaId,
                                  long headerMatchMask) {}

    private record MatchResult(
                               List<int[]> byWorkItem,
                               Set<ContentId> contentIds) {}
}

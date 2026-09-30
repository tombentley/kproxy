/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.record.manipulation.filter.resolve;

import java.nio.ByteBuffer;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.IntFunction;

import org.junit.jupiter.api.Test;

import io.kroxylicious.filter.record.manipulation.filter.Direction;
import io.kroxylicious.filter.record.manipulation.filter.SchemaRegistryClient;
import io.kroxylicious.filter.record.manipulation.filter.config.SchemaRef;
import io.kroxylicious.kafka.common.header.Header;
import io.kroxylicious.kafka.common.record.internal.MemoryRecords;
import io.kroxylicious.kafka.common.record.internal.Record;
import io.kroxylicious.kafka.common.utils.ByteBufferOutputStream;
import io.kroxylicious.testing.filter.record.RecordTestUtils;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;

class ManipulationOrchestratorTest {

    private static final Header[] NO_HEADERS = new Header[0];
    private static final Executor INLINE = Runnable::run;
    private static final IntFunction<ByteBufferOutputStream> BUFFERS = ByteBufferOutputStream::new;
    private static final SchemaRef ORDERS_VALUE = new SchemaRef("com.myco", "orders-value", "1");

    /** A {@link ContextualBufferTransform} that rewrites the value to the record's resolved schema bytes. */
    private static final ContextualBufferTransform TO_SCHEMA_CONTENT = (record, resolved) -> {
        Long globalId = new SchemaIdExtractor(Record::key).extract(record);
        SchemaRegistryClient.VersionsResult versions = resolved.get(SchemaFacets.SCHEMA_VERSIONS, globalId);
        byte[] content = resolved.get(SchemaFacets.SCHEMA_CONTENT, versions.contentId());
        return ByteBuffer.wrap(content);
    };

    private static byte[] framed(long globalId) {
        return ByteBuffer.allocate(1 + Long.BYTES).put((byte) 0x00).putLong(globalId).array();
    }

    private static MemoryRecords recordsFramedWith(long globalId, String... values) {
        List<Record> records = new ArrayList<>();
        for (int i = 0; i < values.length; i++) {
            records.add(RecordTestUtils.record(i, 0L, framed(globalId), values[i].getBytes(UTF_8), NO_HEADERS));
        }
        return RecordTestUtils.memoryRecords(records);
    }

    private static List<String> values(MemoryRecords memoryRecords) {
        List<String> values = new ArrayList<>();
        memoryRecords.records().forEach(record -> values.add(RecordTestUtils.recordValueAsString(record)));
        return values;
    }

    private static CompiledManipulation selectByKeySchema(SchemaRef keySchema, ContextualBufferTransform newValue) {
        return new CompiledManipulation(Direction.IN, name -> true, null, keySchema, null, null, null, newValue);
    }

    @Test
    void resolvesSchemaContentAndAppliesTransformToMatchingRecords() {
        // Given
        FakeSchemaRegistryClient client = new FakeSchemaRegistryClient()
                .withVersion(100L, "com.myco", "orders-value", "1", 500L)
                .withContent(500L, "SCHEMA-500".getBytes(UTF_8));
        ManipulationOrchestrator orchestrator = new ManipulationOrchestrator(new SchemaFacets(client, -1, -1), INLINE);
        AtomicReference<MemoryRecords> written = new AtomicReference<>();
        PartitionWorkItem workItem = new PartitionWorkItem("orders", recordsFramedWith(100L, "a", "b", "c"),
                List.of(selectByKeySchema(ORDERS_VALUE, TO_SCHEMA_CONTENT)), written::set);

        // When
        CompletionStage<Void> result = orchestrator.process(List.of(workItem), BUFFERS);

        // Then
        assertThat(result).succeedsWithin(Duration.ofSeconds(5));
        assertThat(values(written.get())).containsExactly("SCHEMA-500", "SCHEMA-500", "SCHEMA-500");
        assertThat(client.versionCalls).containsExactly(100L);
        assertThat(client.contentCalls).containsExactly(500L);
    }

    @Test
    void resolvesSharedSchemaAcrossPartitionsExactlyOnce() {
        // Given
        FakeSchemaRegistryClient client = new FakeSchemaRegistryClient()
                .withVersion(100L, "com.myco", "orders-value", "1", 500L)
                .withContent(500L, "SCHEMA-500".getBytes(UTF_8));
        ManipulationOrchestrator orchestrator = new ManipulationOrchestrator(new SchemaFacets(client, -1, -1), INLINE);
        CompiledManipulation manipulation = selectByKeySchema(ORDERS_VALUE, TO_SCHEMA_CONTENT);
        AtomicReference<MemoryRecords> partition0 = new AtomicReference<>();
        AtomicReference<MemoryRecords> partition1 = new AtomicReference<>();
        List<PartitionWorkItem> workItems = List.of(
                new PartitionWorkItem("orders", recordsFramedWith(100L, "a", "b"), List.of(manipulation), partition0::set),
                new PartitionWorkItem("orders", recordsFramedWith(100L, "c"), List.of(manipulation), partition1::set));

        // When
        CompletionStage<Void> result = orchestrator.process(workItems, BUFFERS);

        // Then
        assertThat(result).succeedsWithin(Duration.ofSeconds(5));
        assertThat(client.versionCalls).containsExactly(100L);
        assertThat(client.contentCalls).containsExactly(500L);
        assertThat(values(partition0.get())).containsExactly("SCHEMA-500", "SCHEMA-500");
        assertThat(values(partition1.get())).containsExactly("SCHEMA-500");
    }

    @Test
    void leavesRecordsUntouchedWhenNothingMatches() {
        // Given
        FakeSchemaRegistryClient client = new FakeSchemaRegistryClient()
                .withVersion(200L, "other", "unrelated", "1", 999L);
        ManipulationOrchestrator orchestrator = new ManipulationOrchestrator(new SchemaFacets(client, -1, -1), INLINE);
        AtomicReference<MemoryRecords> written = new AtomicReference<>();
        PartitionWorkItem workItem = new PartitionWorkItem("orders", recordsFramedWith(200L, "a", "b"),
                List.of(selectByKeySchema(ORDERS_VALUE, TO_SCHEMA_CONTENT)), written::set);

        // When
        CompletionStage<Void> result = orchestrator.process(List.of(workItem), BUFFERS);

        // Then
        assertThat(result).succeedsWithin(Duration.ofSeconds(5));
        assertThat(written.get()).isNull();
        assertThat(client.contentCalls).isEmpty();
    }

    @Test
    void stageStaysPendingUntilResolutionCompletes() {
        // Given
        CompletableFuture<SchemaRegistryClient.VersionsResult> pendingVersion = new CompletableFuture<>();
        FakeSchemaRegistryClient client = new FakeSchemaRegistryClient()
                .withPendingVersion(100L, pendingVersion)
                .withContent(500L, "SCHEMA-500".getBytes(UTF_8));
        ManipulationOrchestrator orchestrator = new ManipulationOrchestrator(new SchemaFacets(client, -1, -1), INLINE);
        AtomicReference<MemoryRecords> written = new AtomicReference<>();
        PartitionWorkItem workItem = new PartitionWorkItem("orders", recordsFramedWith(100L, "a"),
                List.of(selectByKeySchema(ORDERS_VALUE, TO_SCHEMA_CONTENT)), written::set);

        // When
        CompletionStage<Void> result = orchestrator.process(List.of(workItem), BUFFERS);

        // Then
        assertThat(result.toCompletableFuture()).isNotCompleted();
        pendingVersion.complete(new SchemaRegistryClient.VersionsResult(100L, "com.myco", "orders-value", "1", 500L));
        assertThat(result).succeedsWithin(Duration.ofSeconds(5));
        assertThat(values(written.get())).containsExactly("SCHEMA-500");
    }

    /**
     * A programmable {@link SchemaRegistryClient} that records the ids it is asked to resolve so tests can
     * assert on deduplication.
     */
    private static final class FakeSchemaRegistryClient implements SchemaRegistryClient {
        private final Map<Long, CompletionStage<VersionsResult>> versions = new HashMap<>();
        private final Map<Long, byte[]> contents = new HashMap<>();
        final List<Long> versionCalls = new CopyOnWriteArrayList<>();
        final List<Long> contentCalls = new CopyOnWriteArrayList<>();

        FakeSchemaRegistryClient withVersion(long globalId, String groupId, String artifactId, String version, long contentId) {
            versions.put(globalId, CompletableFuture.completedFuture(new VersionsResult(globalId, groupId, artifactId, version, contentId)));
            return this;
        }

        FakeSchemaRegistryClient withPendingVersion(long globalId, CompletionStage<VersionsResult> pending) {
            versions.put(globalId, pending);
            return this;
        }

        FakeSchemaRegistryClient withContent(long contentId, byte[] bytes) {
            contents.put(contentId, bytes);
            return this;
        }

        @Override
        public CompletionStage<VersionsResult> artifactIdForGlobalId(long globalId) {
            versionCalls.add(globalId);
            return versions.get(globalId);
        }

        @Override
        public CompletionStage<ContentResult> getContentById(long contentId) {
            contentCalls.add(contentId);
            return CompletableFuture.completedFuture(new ContentResult(contentId, contents.get(contentId)));
        }
    }
}

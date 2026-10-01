/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.record.manipulation.filter;

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
import java.util.function.Predicate;
import java.util.stream.StreamSupport;

import org.junit.jupiter.api.Test;

import io.kroxylicious.filter.record.manipulation.filter.config.Direction;
import io.kroxylicious.filter.record.manipulation.filter.config.SchemaRef;
import io.kroxylicious.filter.record.manipulation.filter.resolve.CompiledManipulation;
import io.kroxylicious.filter.record.manipulation.filter.resolve.ContextualBufferTransform;
import io.kroxylicious.filter.record.manipulation.filter.resolve.SchemaFacets;
import io.kroxylicious.kafka.common.Uuid;
import io.kroxylicious.kafka.common.header.Header;
import io.kroxylicious.kafka.common.message.FetchResponseData;
import io.kroxylicious.kafka.common.message.ProduceRequestData;
import io.kroxylicious.kafka.common.message.RequestHeaderData;
import io.kroxylicious.kafka.common.message.ResponseHeaderData;
import io.kroxylicious.kafka.common.message.ShareFetchResponseData;
import io.kroxylicious.kafka.common.protocol.Errors;
import io.kroxylicious.kafka.common.record.internal.BaseRecords;
import io.kroxylicious.kafka.common.record.internal.MemoryRecords;
import io.kroxylicious.kafka.common.record.internal.Record;
import io.kroxylicious.kafka.common.record.internal.Records;
import io.kroxylicious.testing.filter.assertj.MockFilterContextAssert;
import io.kroxylicious.testing.filter.context.MockFilterContext;
import io.kroxylicious.testing.filter.record.RecordTestUtils;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Behavioural tests for the selection/forwarding logic the three filter methods gained when the full
 * configuration model (a list of {@link CompiledManipulation}s) was wired in: choosing which
 * manipulations apply by direction and topic, pooling matching partitions into work items, resolving
 * schema ids and picking the first manipulation whose schema ref matches, and forwarding the message
 * unmodified when nothing matches.
 */
class RecordManipulationFilterTest {

    private static final Header[] NO_HEADERS = new Header[0];
    private static final Executor INLINE = Runnable::run;
    private static final String TOPIC = "orders";
    private static final Uuid TOPIC_ID = new Uuid(7L, 7L);
    private static final SchemaRef ORDERS_KEY = new SchemaRef("com.myco", "orders-key", "1");
    private static final SchemaRef ORDERS_VALUE = new SchemaRef("com.myco", "orders-value", "1");

    // --- Produce (direction IN) ---------------------------------------------------------------

    @Test
    void forwardsProduceRequestUnchangedWhenNoManipulationMatchesTopic() {
        // Given
        var filter = filter(null, topicManipulation(Direction.IN, TOPIC::equals, rewriteValueTo("TRANSFORMED")));
        var request = produceRequest("some-other-topic", "a", "b");
        var header = new RequestHeaderData();
        var context = MockFilterContext.builder(header, request).build();

        // When
        var stage = filter.onProduceRequest(ProduceRequestData.HIGHEST_SUPPORTED_VERSION, header, request, context);

        // Then
        assertThat(stage).succeedsWithin(Duration.ZERO).satisfies(result -> MockFilterContextAssert.assertThat(result)
                .isForwardRequest()
                .hasMessageInstanceOfSatisfying(ProduceRequestData.class, forwarded -> assertThat(produceValues(forwarded)).containsExactly("a", "b")));
    }

    @Test
    void forwardsProduceRequestUnchangedWhenManipulationDirectionIsOut() {
        // Given
        var filter = filter(null, topicManipulation(Direction.OUT, TOPIC::equals, rewriteValueTo("TRANSFORMED")));
        var request = produceRequest(TOPIC, "a", "b");
        var header = new RequestHeaderData();
        var context = MockFilterContext.builder(header, request).build();

        // When
        var stage = filter.onProduceRequest(ProduceRequestData.HIGHEST_SUPPORTED_VERSION, header, request, context);

        // Then
        assertThat(stage).succeedsWithin(Duration.ZERO).satisfies(result -> MockFilterContextAssert.assertThat(result)
                .isForwardRequest()
                .hasMessageInstanceOfSatisfying(ProduceRequestData.class, forwarded -> assertThat(produceValues(forwarded)).containsExactly("a", "b")));
    }

    @Test
    void appliesMatchingManipulationToProduceRequest() {
        // Given
        var filter = filter(null, topicManipulation(Direction.IN, TOPIC::equals, rewriteValueTo("TRANSFORMED")));
        var request = produceRequest(TOPIC, "a", "b");
        var header = new RequestHeaderData();
        var context = MockFilterContext.builder(header, request).build();

        // When
        var stage = filter.onProduceRequest(ProduceRequestData.HIGHEST_SUPPORTED_VERSION, header, request, context);

        // Then
        assertThat(stage).succeedsWithin(Duration.ZERO).satisfies(result -> MockFilterContextAssert.assertThat(result)
                .isForwardRequest()
                .hasMessageInstanceOfSatisfying(ProduceRequestData.class,
                        forwarded -> assertThat(produceValues(forwarded)).containsExactly("TRANSFORMED", "TRANSFORMED")));
    }

    @Test
    void appliesFirstManipulationWhoseSchemaRefMatchesProduceRequest() {
        // Given
        var client = new FakeSchemaRegistryClient().withVersion(100L, "com.myco", "orders-value", "1", 500L);
        var filter = filter(new SchemaFacets(client, -1, -1),
                keySchemaManipulation(Direction.IN, ORDERS_KEY, rewriteValueTo("FIRST")),
                keySchemaManipulation(Direction.IN, ORDERS_VALUE, rewriteValueTo("SECOND")));
        var request = produceRequestFramedKey(TOPIC, 100L, "a", "b");
        var header = new RequestHeaderData();
        var context = MockFilterContext.builder(header, request).build();

        // When
        var stage = filter.onProduceRequest(ProduceRequestData.HIGHEST_SUPPORTED_VERSION, header, request, context);

        // Then
        assertThat(stage).succeedsWithin(Duration.ofSeconds(5)).satisfies(result -> MockFilterContextAssert.assertThat(result)
                .isForwardRequest()
                .hasMessageInstanceOfSatisfying(ProduceRequestData.class,
                        forwarded -> assertThat(produceValues(forwarded)).containsExactly("SECOND", "SECOND")));
        assertThat(client.versionCalls).containsExactly(100L);
    }

    @Test
    void forwardsProduceRequestUnchangedWhenNoManipulationsSchemaRefMatches() {
        // Given
        var client = new FakeSchemaRegistryClient().withVersion(100L, "com.myco", "orders-value", "1", 500L);
        var filter = filter(new SchemaFacets(client, -1, -1),
                keySchemaManipulation(Direction.IN, new SchemaRef("com.myco", "unrelated", "1"), rewriteValueTo("TRANSFORMED")));
        var request = produceRequestFramedKey(TOPIC, 100L, "a", "b");
        var header = new RequestHeaderData();
        var context = MockFilterContext.builder(header, request).build();

        // When
        var stage = filter.onProduceRequest(ProduceRequestData.HIGHEST_SUPPORTED_VERSION, header, request, context);

        // Then
        assertThat(stage).succeedsWithin(Duration.ofSeconds(5)).satisfies(result -> MockFilterContextAssert.assertThat(result)
                .isForwardRequest()
                .hasMessageInstanceOfSatisfying(ProduceRequestData.class, forwarded -> assertThat(produceValues(forwarded)).containsExactly("a", "b")));
    }

    @Test
    void resolvesTopicNameFromTopicIdWhenProduceRequestOmitsName() {
        // Given
        var filter = filter(null, topicManipulation(Direction.IN, TOPIC::equals, rewriteValueTo("TRANSFORMED")));
        var request = produceRequest(TOPIC_ID, "a");
        var header = new RequestHeaderData();
        var context = MockFilterContext.builder(header, request).withTopicName(TOPIC_ID, TOPIC).build();

        // When
        var stage = filter.onProduceRequest(ProduceRequestData.HIGHEST_SUPPORTED_VERSION, header, request, context);

        // Then
        assertThat(stage).succeedsWithin(Duration.ZERO).satisfies(result -> MockFilterContextAssert.assertThat(result)
                .isForwardRequest()
                .hasMessageInstanceOfSatisfying(ProduceRequestData.class, forwarded -> assertThat(produceValues(forwarded)).containsExactly("TRANSFORMED")));
    }

    // --- Fetch (direction OUT) ----------------------------------------------------------------

    @Test
    void appliesMatchingManipulationToFetchResponse() {
        // Given
        var filter = filter(null, topicManipulation(Direction.OUT, TOPIC::equals, rewriteValueTo("TRANSFORMED")));
        var response = fetchResponse(TOPIC, "a", "b");
        var header = new ResponseHeaderData();
        var context = MockFilterContext.builder(header, response).build();

        // When
        var stage = filter.onFetchResponse(FetchResponseData.HIGHEST_SUPPORTED_VERSION, header, response, context);

        // Then
        assertThat(stage).succeedsWithin(Duration.ZERO).satisfies(result -> MockFilterContextAssert.assertThat(result)
                .isForwardResponse()
                .hasMessageInstanceOfSatisfying(FetchResponseData.class,
                        forwarded -> assertThat(fetchValues(forwarded)).containsExactly("TRANSFORMED", "TRANSFORMED")));
    }

    @Test
    void forwardsFetchResponseUnchangedWhenManipulationDirectionIsIn() {
        // Given
        var filter = filter(null, topicManipulation(Direction.IN, TOPIC::equals, rewriteValueTo("TRANSFORMED")));
        var response = fetchResponse(TOPIC, "a", "b");
        var header = new ResponseHeaderData();
        var context = MockFilterContext.builder(header, response).build();

        // When
        var stage = filter.onFetchResponse(FetchResponseData.HIGHEST_SUPPORTED_VERSION, header, response, context);

        // Then
        assertThat(stage).succeedsWithin(Duration.ZERO).satisfies(result -> MockFilterContextAssert.assertThat(result)
                .isForwardResponse()
                .hasMessageInstanceOfSatisfying(FetchResponseData.class, forwarded -> assertThat(fetchValues(forwarded)).containsExactly("a", "b")));
    }

    // --- ShareFetch (direction OUT) -----------------------------------------------------------

    @Test
    void appliesMatchingManipulationToShareFetchResponse() {
        // Given
        var filter = filter(null, topicManipulation(Direction.OUT, TOPIC::equals, rewriteValueTo("TRANSFORMED")));
        var response = shareFetchResponse(TOPIC_ID, "a", "b");
        var header = new ResponseHeaderData();
        var context = MockFilterContext.builder(header, response).withTopicName(TOPIC_ID, TOPIC).build();

        // When
        var stage = filter.onShareFetchResponse(ShareFetchResponseData.HIGHEST_SUPPORTED_VERSION, header, response, context);

        // Then
        assertThat(stage).succeedsWithin(Duration.ZERO).satisfies(result -> MockFilterContextAssert.assertThat(result)
                .isForwardResponse()
                .hasMessageInstanceOfSatisfying(ShareFetchResponseData.class,
                        forwarded -> assertThat(shareFetchValues(forwarded)).containsExactly("TRANSFORMED", "TRANSFORMED")));
    }

    @Test
    void setsUnknownTopicIdErrorOnShareFetchResponseForUnresolvableTopic() {
        // Given
        var filter = filter(null, topicManipulation(Direction.OUT, TOPIC::equals, rewriteValueTo("TRANSFORMED")));
        var response = shareFetchResponse(TOPIC_ID, "a");
        var header = new ResponseHeaderData();
        var context = MockFilterContext.builder(header, response).build();

        // When
        var stage = filter.onShareFetchResponse(ShareFetchResponseData.HIGHEST_SUPPORTED_VERSION, header, response, context);

        // Then
        assertThat(stage).succeedsWithin(Duration.ZERO).satisfies(result -> MockFilterContextAssert.assertThat(result)
                .isForwardResponse()
                .hasMessageInstanceOfSatisfying(ShareFetchResponseData.class, forwarded -> {
                    var topicResponse = forwarded.responses().stream().findFirst().orElseThrow();
                    var partition = topicResponse.partitions().get(0);
                    assertThat(partition.errorCode()).isEqualTo(Errors.UNKNOWN_TOPIC_ID.code());
                    assertThat(partition.records()).isNull();
                }));
    }

    // --- helpers ------------------------------------------------------------------------------

    private static RecordManipulationFilter filter(SchemaFacets schemaFacets, CompiledManipulation... manipulations) {
        return new RecordManipulationFilter(new Init(List.of(manipulations), schemaFacets), INLINE, schemaFacets);
    }

    private static CompiledManipulation topicManipulation(Direction direction, Predicate<String> topicMatcher, ContextualBufferTransform newValue) {
        return new CompiledManipulation(direction, topicMatcher, null, null, null, null, null, null, null, null, null, null, null, newValue);
    }

    private static CompiledManipulation keySchemaManipulation(Direction direction, SchemaRef keySchema, ContextualBufferTransform newValue) {
        return new CompiledManipulation(direction, name -> true, null, keySchema, null, null, null, null, null, null, null, null, null, newValue);
    }

    private static ContextualBufferTransform rewriteValueTo(String replacement) {
        byte[] bytes = replacement.getBytes(UTF_8);
        return (record, resolved) -> ByteBuffer.wrap(bytes);
    }

    private static byte[] framed(long globalId) {
        return ByteBuffer.allocate(1 + Long.BYTES).put((byte) 0x00).putLong(globalId).array();
    }

    private static MemoryRecords records(byte[] key, String... values) {
        List<Record> records = new ArrayList<>();
        for (int i = 0; i < values.length; i++) {
            records.add(RecordTestUtils.record(i, 0L, key, values[i].getBytes(UTF_8), NO_HEADERS));
        }
        return RecordTestUtils.memoryRecords(records);
    }

    private static ProduceRequestData produceRequest(String topicName, String... values) {
        var request = new ProduceRequestData();
        request.topicData().add(new ProduceRequestData.TopicProduceData()
                .setName(topicName)
                .setPartitionData(List.of(new ProduceRequestData.PartitionProduceData().setRecords(records(null, values)))));
        return request;
    }

    private static ProduceRequestData produceRequest(Uuid topicId, String... values) {
        var request = new ProduceRequestData();
        request.topicData().add(new ProduceRequestData.TopicProduceData()
                .setTopicId(topicId)
                .setPartitionData(List.of(new ProduceRequestData.PartitionProduceData().setRecords(records(null, values)))));
        return request;
    }

    private static ProduceRequestData produceRequestFramedKey(String topicName, long globalId, String... values) {
        var request = new ProduceRequestData();
        request.topicData().add(new ProduceRequestData.TopicProduceData()
                .setName(topicName)
                .setPartitionData(List.of(new ProduceRequestData.PartitionProduceData().setRecords(records(framed(globalId), values)))));
        return request;
    }

    private static FetchResponseData fetchResponse(String topicName, String... values) {
        var response = new FetchResponseData();
        response.responses().add(new FetchResponseData.FetchableTopicResponse()
                .setTopic(topicName)
                .setPartitions(List.of(new FetchResponseData.PartitionData().setRecords(records(null, values)))));
        return response;
    }

    private static ShareFetchResponseData shareFetchResponse(Uuid topicId, String... values) {
        var response = new ShareFetchResponseData();
        response.responses().add(new ShareFetchResponseData.ShareFetchableTopicResponse()
                .setTopicId(topicId)
                .setPartitions(List.of(new ShareFetchResponseData.PartitionData().setRecords(records(null, values)))));
        return response;
    }

    private static List<String> produceValues(ProduceRequestData request) {
        return request.topicData().stream()
                .flatMap(t -> t.partitionData().stream())
                .flatMap(p -> recordValues(p.records()).stream())
                .toList();
    }

    private static List<String> fetchValues(FetchResponseData response) {
        return response.responses().stream()
                .flatMap(t -> t.partitions().stream())
                .flatMap(p -> recordValues(p.records()).stream())
                .toList();
    }

    private static List<String> shareFetchValues(ShareFetchResponseData response) {
        return response.responses().stream()
                .flatMap(t -> t.partitions().stream())
                .flatMap(p -> recordValues(p.records()).stream())
                .toList();
    }

    private static List<String> recordValues(BaseRecords records) {
        List<String> values = new ArrayList<>();
        StreamSupport.stream(((Records) records).records().spliterator(), false)
                .forEach(record -> values.add(RecordTestUtils.recordValueAsString(record)));
        return values;
    }

    /**
     * A programmable {@link SchemaRegistryClient} recording the global ids it was asked to resolve, mirroring the fake
     * used by {@code ManipulationOrchestratorTest} so these filter tests stay self-contained.
     */
    private static final class FakeSchemaRegistryClient implements SchemaRegistryClient {
        private final Map<Long, VersionsResult> versions = new HashMap<>();
        private final Map<Long, byte[]> contents = new HashMap<>();
        final List<Long> versionCalls = new CopyOnWriteArrayList<>();
        final List<Long> contentCalls = new CopyOnWriteArrayList<>();

        FakeSchemaRegistryClient withVersion(long globalId, String groupId, String artifactId, String version, long contentId) {
            versions.put(globalId, new VersionsResult(globalId, groupId, artifactId, version, contentId));
            return this;
        }

        @Override
        public CompletionStage<VersionsResult> artifactIdForGlobalId(long globalId) {
            versionCalls.add(globalId);
            return CompletableFuture.completedFuture(versions.get(globalId));
        }

        @Override
        public CompletionStage<ContentResult> getContentById(long contentId) {
            contentCalls.add(contentId);
            return CompletableFuture.completedFuture(new ContentResult(contentId, contents.get(contentId)));
        }
    }
}

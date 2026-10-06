/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.record.manipulation.filter;

import java.nio.ByteBuffer;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.function.Predicate;
import java.util.stream.StreamSupport;

import org.junit.jupiter.api.Test;

import io.kroxylicious.filter.record.manipulation.filter.config.Direction;
import io.kroxylicious.filter.record.manipulation.filter.resolve.CompiledManipulation;
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
import io.kroxylicious.transform.service.BufferTransform;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Behavioural tests for the three filter methods driving the {@code records} transform engine: choosing which
 * manipulations apply by direction and topic, transforming each matching partition's records, and forwarding
 * the message unmodified when nothing matches.
 */
class RecordManipulationFilterTest {

    private static final Header[] NO_HEADERS = new Header[0];
    private static final Executor INLINE = Runnable::run;
    private static final String TOPIC = "orders";
    private static final Uuid TOPIC_ID = new Uuid(7L, 7L);

    // --- Produce (direction IN) ---------------------------------------------------------------

    @Test
    void forwardsProduceRequestUnchangedWhenNoManipulationMatchesTopic() {
        // Given
        var filter = filter(topicManipulation(Direction.INTO_CLUSTER, TOPIC::equals, rewriteValueTo("TRANSFORMED")));
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
        var filter = filter(topicManipulation(Direction.OUT_OF_CLUSTER, TOPIC::equals, rewriteValueTo("TRANSFORMED")));
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
        var filter = filter(topicManipulation(Direction.INTO_CLUSTER, TOPIC::equals, rewriteValueTo("TRANSFORMED")));
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
    void resolvesTopicNameFromTopicIdWhenProduceRequestOmitsName() {
        // Given
        var filter = filter(topicManipulation(Direction.INTO_CLUSTER, TOPIC::equals, rewriteValueTo("TRANSFORMED")));
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
        var filter = filter(topicManipulation(Direction.OUT_OF_CLUSTER, TOPIC::equals, rewriteValueTo("TRANSFORMED")));
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
        var filter = filter(topicManipulation(Direction.INTO_CLUSTER, TOPIC::equals, rewriteValueTo("TRANSFORMED")));
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
        var filter = filter(topicManipulation(Direction.OUT_OF_CLUSTER, TOPIC::equals, rewriteValueTo("TRANSFORMED")));
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
        var filter = filter(topicManipulation(Direction.OUT_OF_CLUSTER, TOPIC::equals, rewriteValueTo("TRANSFORMED")));
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

    private static RecordManipulationFilter filter(CompiledManipulation<?, ?, ?>... manipulations) {
        return new RecordManipulationFilter(new Init(List.of(manipulations), null), INLINE);
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private static CompiledManipulation<?, ?, ?> topicManipulation(Direction direction, Predicate<String> topicMatcher, BufferTransform newValue) {
        return new CompiledManipulation(direction, topicMatcher, subject -> true, null, null, null, null, null, null, null, null, null, null, null, newValue);
    }

    private static BufferTransform rewriteValueTo(String replacement) {
        byte[] bytes = replacement.getBytes(UTF_8);
        return record -> ByteBuffer.wrap(bytes);
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
}

/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.transform.neo;

import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.kroxylicious.filter.record.manipulation.filter.config.Direction;
import io.kroxylicious.filter.record.manipulation.filter.RecordManipulation;
import io.kroxylicious.filter.record.manipulation.filter.RecordManipulationFilter;
import io.kroxylicious.filter.record.manipulation.filter.config.Manipulation;
import io.kroxylicious.filter.record.manipulation.filter.config.RecordManipulationConfig;
import io.kroxylicious.filter.record.manipulation.filter.config.Then;
import io.kroxylicious.filter.record.manipulation.filter.config.TopicName;
import io.kroxylicious.filter.record.manipulation.filter.config.When;
import io.kroxylicious.kafka.common.message.ProduceRequestData;
import io.kroxylicious.kafka.common.message.ProduceRequestData.PartitionProduceData;
import io.kroxylicious.kafka.common.message.ProduceRequestData.TopicProduceData;
import io.kroxylicious.kafka.common.message.RequestHeaderData;
import io.kroxylicious.kafka.common.record.internal.Record;
import io.kroxylicious.kafka.common.record.internal.Records;
import io.kroxylicious.proxy.config.ServiceBasedPluginFactoryRegistry;
import io.kroxylicious.proxy.filter.FilterFactoryContext;
import io.kroxylicious.testing.filter.assertj.MockFilterContextAssert;
import io.kroxylicious.testing.filter.context.MockFilterContext;
import io.kroxylicious.testing.filter.record.RecordTestUtils;
import io.kroxylicious.transform.neo.format.jackson2.JsonTransform;
import io.kroxylicious.transform.neo.format.jackson2.ReadJson;
import io.kroxylicious.transform.neo.format.jackson2.WriteJson;
import io.kroxylicious.transform.neo.op.OpConfig;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * End-to-end test of {@link RecordManipulation}/{@link RecordManipulationFilter}: builds a filter from a
 * {@link RecordManipulationConfig} the way the real proxy would (via {@link RecordManipulation#initialize}
 * then {@link RecordManipulation#createFilter}), and drives it with a produce request carrying a JSON
 * record value.
 */
@ExtendWith(MockitoExtension.class)
class RecordManipulationFilterTest {

    private static final String TOPIC_NAME = "mytopic";
    private static final String RECORD_KEY = "mykey";
    private static final String ORIGINAL_VALUE = """
            {"firstName":"Harry","ageYears":17}""";
    private static final ObjectMapper JSON = new ObjectMapper();

    private final ServiceBasedPluginFactoryRegistry registry = new ServiceBasedPluginFactoryRegistry();

    @Mock(strictness = Mock.Strictness.LENIENT)
    FilterFactoryContext factoryContext;

    @BeforeEach
    void setUp() {
        when(factoryContext.pluginInstance(any(), any()))
                .thenAnswer(invocation -> registry.pluginFactory(invocation.<Class<?>> getArgument(0))
                        .pluginInstance(invocation.getArgument(1)));
    }

    private static PipelineConfig jsonMaskingValuePipeline() {
        Map<String, Object> schema = Map.of(
                "type", "object",
                "properties", Map.of(
                        "firstName", Map.of(
                                "type", "string",
                                "apply", List.of(Map.of("op", "ValueString", "value", "REDACTED"))),
                        "ageYears", Map.of("type", "integer")));
        return new PipelineConfig(Origin.RecordValue, List.of(
                new OpConfig(ReadJson.class),
                new OpConfig(JsonTransform.class, Map.of("withKeySchema", schema)),
                new OpConfig(WriteJson.class)));
    }

    private RecordManipulationFilter buildFilter(Direction direction) {
        var config = new RecordManipulationConfig(null,
                List.of(new Manipulation(
                            new When(direction,
                                TopicName.is(TOPIC_NAME),
                    null, null, null),
                            new Then(null, null,
                                null, null,
                                BufferPipeline.class.getName(), jsonMaskingValuePipeline()))));
        var factory = new RecordManipulation();
        var init = factory.initialize(factoryContext, config);
        return (RecordManipulationFilter) factory.createFilter(factoryContext, init);
    }

    @Test
    void masksJsonFieldOnMatchingTopicAndDirection() {
        // Given
        var filter = buildFilter(Direction.IN);
        var produceRequest = produceRequestWithOneRecord(TOPIC_NAME, RECORD_KEY, ORIGINAL_VALUE);
        var header = new RequestHeaderData();
        var mockFilterContext = MockFilterContext.builder(header, produceRequest).build();

        // When
        var stage = filter.onProduceRequest(produceRequest.apiKey(), header, produceRequest, mockFilterContext);

        // Then
        assertThat(stage).succeedsWithin(Duration.ZERO).satisfies(result -> {
            MockFilterContextAssert.assertThat(result)
                    .isForwardRequest().hasMessageInstanceOfSatisfying(ProduceRequestData.class, filteredRequest -> {
                        var record = onlyRecord(filteredRequest);

                        JsonNode maskedValue = readValue(record);
                        assertThat(maskedValue.get("firstName").asText())
                                .withFailMessage("expected firstName to have been masked")
                                .isEqualTo("REDACTED");
                        assertThat(maskedValue.get("ageYears").asInt())
                                .withFailMessage("expected ageYears to pass through unchanged (no apply chain configured for it)")
                                .isEqualTo(17);

                        assertThat(decodeUtf8Key(record))
                                .withFailMessage("expected the key to pass through unchanged (no intoRecordKey configured, falls back to identity)")
                                .isEqualTo(RECORD_KEY);
                    });
        });
    }

    @Test
    void passesThroughRecordsOnNonMatchingTopic() {
        // Given
        var filter = buildFilter(Direction.IN);
        var produceRequest = produceRequestWithOneRecord("some-other-topic", RECORD_KEY, ORIGINAL_VALUE);
        var header = new RequestHeaderData();
        var mockFilterContext = MockFilterContext.builder(header, produceRequest).build();

        // When
        var stage = filter.onProduceRequest(produceRequest.apiKey(), header, produceRequest, mockFilterContext);

        // Then
        assertThat(stage).succeedsWithin(Duration.ZERO).satisfies(result -> {
            MockFilterContextAssert.assertThat(result)
                    .isForwardRequest().hasMessageInstanceOfSatisfying(ProduceRequestData.class, filteredRequest -> {
                        var record = onlyRecord(filteredRequest);
                        assertThat(decodeUtf8Value(record))
                                .withFailMessage("expected the value to be untouched for a non-matching topic")
                                .isEqualTo(ORIGINAL_VALUE);
                    });
        });
    }

    @Test
    void passesThroughRecordsOnNonMatchingDirection() {
        // Given
        var filter = buildFilter(Direction.OUT);
        var produceRequest = produceRequestWithOneRecord(TOPIC_NAME, RECORD_KEY, ORIGINAL_VALUE);
        var header = new RequestHeaderData();
        var mockFilterContext = MockFilterContext.builder(header, produceRequest).build();

        // When
        var stage = filter.onProduceRequest(produceRequest.apiKey(), header, produceRequest, mockFilterContext);

        // Then
        assertThat(stage).succeedsWithin(Duration.ZERO).satisfies(result -> {
            MockFilterContextAssert.assertThat(result)
                    .isForwardRequest().hasMessageInstanceOfSatisfying(ProduceRequestData.class, filteredRequest -> {
                        var record = onlyRecord(filteredRequest);
                        assertThat(decodeUtf8Value(record))
                                .withFailMessage("expected the value to be untouched: this filter instance only handles OUT (fetch) traffic")
                                .isEqualTo(ORIGINAL_VALUE);
                    });
        });
    }

    private static ProduceRequestData produceRequestWithOneRecord(String topicName, String key, String value) {
        var produceRequest = new ProduceRequestData();
        var topicProduceData = new TopicProduceData().setName(topicName);
        var partitionData = new PartitionProduceData();
        partitionData.setRecords(RecordTestUtils.singleElementMemoryRecords(key, value));
        topicProduceData.partitionData().add(partitionData);
        produceRequest.topicData().add(topicProduceData);
        return produceRequest;
    }

    private static Record onlyRecord(ProduceRequestData request) {
        var records = requestToRecordStream(request).toList();
        assertThat(records).hasSize(1);
        return records.get(0);
    }

    private static Stream<Record> requestToRecordStream(ProduceRequestData request) {
        return request.topicData().stream()
                .map(TopicProduceData::partitionData)
                .flatMap(Collection::stream)
                .map(PartitionProduceData::records)
                .map(Records.class::cast)
                .map(Records::records)
                .map(Iterable::spliterator)
                .flatMap(si -> StreamSupport.stream(si, false));
    }

    private static String decodeUtf8Value(Record record) {
        return StandardCharsets.UTF_8.decode(record.value()).toString();
    }

    private static String decodeUtf8Key(Record record) {
        return StandardCharsets.UTF_8.decode(record.key()).toString();
    }

    private static JsonNode readValue(Record record) {
        try {
            return JSON.readTree(decodeUtf8Value(record));
        }
        catch (JsonProcessingException e) {
            throw new UncheckedIOException(e);
        }
    }

}

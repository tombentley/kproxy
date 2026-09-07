/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.record.manipulation.filter;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.apache.kafka.common.message.ProduceRequestData;
import org.apache.kafka.common.message.ProduceRequestData.PartitionProduceData;
import org.apache.kafka.common.message.ProduceRequestData.TopicProduceData;
import org.apache.kafka.common.message.RequestHeaderData;
import org.apache.kafka.common.record.Record;
import org.apache.kafka.common.record.Records;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.google.protobuf.Descriptors;
import com.google.protobuf.DynamicMessage;

import io.kroxylicious.filter.record.manipulation.format.protobuf.DeserializeProtobuf;
import io.kroxylicious.filter.record.manipulation.format.protobuf.ProtobufBinaryDeserializer;
import io.kroxylicious.filter.record.manipulation.format.protobuf.ProtobufBinarySerializer;
import io.kroxylicious.filter.record.manipulation.format.protobuf.ProtobufSchemaParser;
import io.kroxylicious.filter.record.manipulation.format.protobuf.ProtobufTransform;
import io.kroxylicious.filter.record.manipulation.format.protobuf.SerializeProtobuf;
import io.kroxylicious.filter.record.manipulation.kafka.Origin;
import io.kroxylicious.filter.record.manipulation.kafka.PipelineConfig;
import io.kroxylicious.filter.record.manipulation.kafka.RecordTransformConfig;
import io.kroxylicious.filter.record.manipulation.op.OpConfig;
import io.kroxylicious.proxy.config.ServiceBasedPluginFactoryRegistry;
import io.kroxylicious.proxy.filter.FilterFactoryContext;
import io.kroxylicious.testing.filter.assertj.MockFilterContextAssert;
import io.kroxylicious.testing.filter.context.MockFilterContext;
import io.kroxylicious.testing.filter.record.RecordTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * End-to-end test of {@link RecordManipulation}/{@link RecordManipulationFilter}: builds a filter from a
 * {@link RecordManipulationConfig} the way the real proxy would (via {@link RecordManipulation#initialize}
 * then {@link RecordManipulation#createFilter}), and drives it with a produce request carrying a
 * Protobuf-binary-encoded record value. Mirrors {@link RecordManipulationFilterAvroTest}, but for the
 * {@link DeserializeProtobuf}/{@link ProtobufTransform}/{@link SerializeProtobuf} pipeline instead of the
 * Avro one.
 */
@ExtendWith(MockitoExtension.class)
class RecordManipulationFilterProtobufTest {

    private static final String TOPIC_NAME = "mytopic";
    private static final String RECORD_KEY = "mykey";
    private static final String SCHEMA_PROTO = """
            syntax = "proto3";
            message User {
                string first_name = 1 [(apply) = { op: "ValueString", value: "REDACTED" }];
                int32 age_years = 2;
            }
            """;
    private static final String ROOT_MESSAGE_NAME = "User";
    private static final Descriptors.Descriptor DESCRIPTOR = ProtobufSchemaParser.parse(SCHEMA_PROTO, ROOT_MESSAGE_NAME).descriptor();

    private final ServiceBasedPluginFactoryRegistry registry = new ServiceBasedPluginFactoryRegistry();

    @Mock(strictness = Mock.Strictness.LENIENT)
    FilterFactoryContext factoryContext;

    @BeforeEach
    void setUp() {
        when(factoryContext.pluginInstance(any(), any()))
                .thenAnswer(invocation -> registry.pluginFactory(invocation.<Class<?>> getArgument(0))
                        .pluginInstance(invocation.getArgument(1)));
    }

    private static PipelineConfig protobufMaskingValuePipeline() {
        Map<String, Object> schemaConfig = Map.of("protoText", SCHEMA_PROTO, "rootMessageName", ROOT_MESSAGE_NAME);
        return new PipelineConfig(Origin.RecordValue, List.of(
                new OpConfig(DeserializeProtobuf.class, schemaConfig),
                new OpConfig(ProtobufTransform.class),
                new OpConfig(SerializeProtobuf.class)));
    }

    private RecordManipulationFilter buildFilter(Direction direction) {
        var config = new RecordManipulationConfig(TOPIC_NAME, direction,
                new RecordTransformConfig(null, null, protobufMaskingValuePipeline()));
        var factory = new RecordManipulation();
        var init = factory.initialize(factoryContext, config);
        return (RecordManipulationFilter) factory.createFilter(factoryContext, init);
    }

    private static byte[] originalValueBytes() {
        DynamicMessage message = DynamicMessage.newBuilder(DESCRIPTOR)
                .setField(DESCRIPTOR.findFieldByName("first_name"), "Harry")
                .setField(DESCRIPTOR.findFieldByName("age_years"), 17)
                .build();
        return new ProtobufBinarySerializer().serialize(message).array();
    }

    @Test
    void masksProtobufFieldOnMatchingTopicAndDirection() {
        // Given
        var filter = buildFilter(Direction.IN);
        var produceRequest = produceRequestWithOneRecord(TOPIC_NAME, RECORD_KEY, originalValueBytes());
        var header = new RequestHeaderData();
        var mockFilterContext = MockFilterContext.builder(header, produceRequest).build();

        // When
        var stage = filter.onProduceRequest(produceRequest.apiKey(), header, produceRequest, mockFilterContext);

        // Then
        assertThat(stage).succeedsWithin(Duration.ZERO).satisfies(result -> {
            MockFilterContextAssert.assertThat(result)
                    .isForwardRequest().hasMessageInstanceOfSatisfying(ProduceRequestData.class, filteredRequest -> {
                        var record = onlyRecord(filteredRequest);

                        DynamicMessage maskedValue = readValue(record);
                        assertThat(maskedValue.getField(DESCRIPTOR.findFieldByName("first_name")))
                                .withFailMessage("expected first_name to have been masked")
                                .isEqualTo("REDACTED");
                        assertThat(maskedValue.getField(DESCRIPTOR.findFieldByName("age_years")))
                                .withFailMessage("expected age_years to pass through unchanged (no apply chain configured for it)")
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
        var originalValue = originalValueBytes();
        var produceRequest = produceRequestWithOneRecord("some-other-topic", RECORD_KEY, originalValue);
        var header = new RequestHeaderData();
        var mockFilterContext = MockFilterContext.builder(header, produceRequest).build();

        // When
        var stage = filter.onProduceRequest(produceRequest.apiKey(), header, produceRequest, mockFilterContext);

        // Then
        assertThat(stage).succeedsWithin(Duration.ZERO).satisfies(result -> {
            MockFilterContextAssert.assertThat(result)
                    .isForwardRequest().hasMessageInstanceOfSatisfying(ProduceRequestData.class, filteredRequest -> {
                        var record = onlyRecord(filteredRequest);
                        assertThat(RecordTestUtils.recordValueAsBytes(record))
                                .withFailMessage("expected the value to be untouched for a non-matching topic")
                                .isEqualTo(originalValue);
                    });
        });
    }

    @Test
    void passesThroughRecordsOnNonMatchingDirection() {
        // Given
        var filter = buildFilter(Direction.OUT);
        var originalValue = originalValueBytes();
        var produceRequest = produceRequestWithOneRecord(TOPIC_NAME, RECORD_KEY, originalValue);
        var header = new RequestHeaderData();
        var mockFilterContext = MockFilterContext.builder(header, produceRequest).build();

        // When
        var stage = filter.onProduceRequest(produceRequest.apiKey(), header, produceRequest, mockFilterContext);

        // Then
        assertThat(stage).succeedsWithin(Duration.ZERO).satisfies(result -> {
            MockFilterContextAssert.assertThat(result)
                    .isForwardRequest().hasMessageInstanceOfSatisfying(ProduceRequestData.class, filteredRequest -> {
                        var record = onlyRecord(filteredRequest);
                        assertThat(RecordTestUtils.recordValueAsBytes(record))
                                .withFailMessage("expected the value to be untouched: this filter instance only handles OUT (fetch) traffic")
                                .isEqualTo(originalValue);
                    });
        });
    }

    private static ProduceRequestData produceRequestWithOneRecord(String topicName, String key, byte[] value) {
        var produceRequest = new ProduceRequestData();
        var topicProduceData = new TopicProduceData().setName(topicName);
        var partitionData = new PartitionProduceData();
        partitionData.setRecords(RecordTestUtils.singleElementMemoryRecords(key.getBytes(StandardCharsets.UTF_8), value));
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

    private static String decodeUtf8Key(Record record) {
        return StandardCharsets.UTF_8.decode(record.key()).toString();
    }

    private static DynamicMessage readValue(Record record) {
        return new ProtobufBinaryDeserializer(DESCRIPTOR).deserialize(record.value());
    }

}

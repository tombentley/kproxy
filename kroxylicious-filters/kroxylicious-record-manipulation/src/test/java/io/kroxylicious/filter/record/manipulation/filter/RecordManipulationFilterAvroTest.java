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

import org.apache.avro.Schema;
import org.apache.avro.generic.GenericData;
import org.apache.avro.generic.GenericRecord;
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

import io.kroxylicious.filter.record.manipulation.format.avro.AvroBinaryDeserializer;
import io.kroxylicious.filter.record.manipulation.format.avro.AvroBinarySerializer;
import io.kroxylicious.filter.record.manipulation.format.avro.AvroTransform;
import io.kroxylicious.filter.record.manipulation.format.avro.DeserializeAvro;
import io.kroxylicious.filter.record.manipulation.format.avro.SerializeAvro;
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
 * then {@link RecordManipulation#createFilter}), and drives it with a produce request carrying an
 * Avro-binary-encoded record value. Mirrors {@link RecordManipulationFilterTest}, but for the
 * {@link DeserializeAvro}/{@link AvroTransform}/{@link SerializeAvro} pipeline instead of the JSON one.
 */
@ExtendWith(MockitoExtension.class)
class RecordManipulationFilterAvroTest {

    private static final String TOPIC_NAME = "mytopic";
    private static final String RECORD_KEY = "mykey";
    private static final String SCHEMA_JSON = """
            {"type": "record", "name": "User", "fields": [
                {"name": "firstName", "type": "string", "apply": [{"op": "ValueString", "value": "REDACTED"}]},
                {"name": "ageYears", "type": "int"}
            ]}
            """;
    private static final Schema SCHEMA = new Schema.Parser().parse(SCHEMA_JSON);

    private final ServiceBasedPluginFactoryRegistry registry = new ServiceBasedPluginFactoryRegistry();

    @Mock(strictness = Mock.Strictness.LENIENT)
    FilterFactoryContext factoryContext;

    @BeforeEach
    void setUp() {
        when(factoryContext.pluginInstance(any(), any()))
                .thenAnswer(invocation -> registry.pluginFactory(invocation.<Class<?>> getArgument(0))
                        .pluginInstance(invocation.getArgument(1)));
    }

    private static PipelineConfig avroMaskingValuePipeline() {
        return new PipelineConfig(Origin.RecordValue, List.of(
                new OpConfig(DeserializeAvro.class, Map.of("schema", SCHEMA_JSON)),
                new OpConfig(AvroTransform.class),
                new OpConfig(SerializeAvro.class)));
    }

    private RecordManipulationFilter buildFilter(Direction direction) {
        var config = new RecordManipulationConfig(TOPIC_NAME, direction,
                new RecordTransformConfig(null, null, avroMaskingValuePipeline()));
        var factory = new RecordManipulation();
        var init = factory.initialize(factoryContext, config);
        return (RecordManipulationFilter) factory.createFilter(factoryContext, init);
    }

    private static byte[] originalValueBytes() {
        GenericRecord record = new GenericData.Record(SCHEMA);
        record.put("firstName", "Harry");
        record.put("ageYears", 17);
        return new AvroBinarySerializer(SCHEMA).serialize(record).array();
    }

    @Test
    void masksAvroFieldOnMatchingTopicAndDirection() {
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

                        GenericRecord maskedValue = readValue(record);
                        assertThat(maskedValue.get("firstName").toString())
                                .withFailMessage("expected firstName to have been masked")
                                .isEqualTo("REDACTED");
                        assertThat(maskedValue.get("ageYears"))
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

    private static GenericRecord readValue(Record record) {
        return (GenericRecord) new AvroBinaryDeserializer(SCHEMA).deserialize(record.value());
    }

    @Test
    void masksEachElementOfAnArrayRootValue() {
        // Given
        String arraySchemaJson = """
                {"type": "array", "items": {"type": "string", "apply": [{"op": "ValueString", "value": "REDACTED"}]}}
                """;
        Schema arraySchema = new Schema.Parser().parse(arraySchemaJson);
        var config = new RecordManipulationConfig(TOPIC_NAME, Direction.IN,
                new RecordTransformConfig(null, null, new PipelineConfig(Origin.RecordValue, List.of(
                        new OpConfig(DeserializeAvro.class, Map.of("schema", arraySchemaJson)),
                        new OpConfig(AvroTransform.class),
                        new OpConfig(SerializeAvro.class)))));
        var factory = new RecordManipulation();
        var init = factory.initialize(factoryContext, config);
        var filter = (RecordManipulationFilter) factory.createFilter(factoryContext, init);

        byte[] originalValue = new AvroBinarySerializer(arraySchema).serialize(List.of("Vernon Dudley", "Barny Weasley")).array();
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
                        List<?> maskedValue = (List<?>) new AvroBinaryDeserializer(arraySchema).deserialize(record.value());
                        assertThat(maskedValue).extracting(Object::toString)
                                .withFailMessage("expected every array element to have been masked")
                                .containsExactly("REDACTED", "REDACTED");
                    });
        });
    }

}

/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.kroxylicious.it.filter.manipulation;

import java.nio.ByteBuffer;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.Serdes;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import com.google.protobuf.Descriptors;
import com.google.protobuf.DynamicMessage;

import io.kroxylicious.filter.record.manipulation.filter.Direction;
import io.kroxylicious.filter.record.manipulation.filter.RecordManipulation;
import io.kroxylicious.filter.record.manipulation.format.protobuf.ProtobufBinaryDeserializer;
import io.kroxylicious.filter.record.manipulation.format.protobuf.ProtobufBinarySerializer;
import io.kroxylicious.filter.record.manipulation.format.protobuf.ProtobufSchemaParser;
import io.kroxylicious.it.BaseIT;
import io.kroxylicious.proxy.config.NamedFilterDefinition;
import io.kroxylicious.testing.integration.config.NamedFilterDefinitionBuilder;
import io.kroxylicious.testing.integration.tester.KroxyliciousTester;
import io.kroxylicious.testing.kafka.api.KafkaCluster;
import io.kroxylicious.testing.kafka.junit5ext.KafkaClusterExtension;
import io.kroxylicious.testing.kafka.junit5ext.Topic;

import static io.kroxylicious.testing.integration.tester.KroxyliciousConfigUtils.proxy;
import static io.kroxylicious.testing.integration.tester.KroxyliciousTesters.kroxyliciousTester;
import static org.apache.kafka.clients.consumer.ConsumerConfig.AUTO_OFFSET_RESET_CONFIG;
import static org.apache.kafka.clients.consumer.ConsumerConfig.GROUP_ID_CONFIG;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * End-to-end test of {@link RecordManipulation}, driving a real Kafka cluster through the proxy. Mirrors
 * the scenarios covered against a mocked filter context in
 * {@code RecordManipulationFilterProtobufTest} (in {@code kroxylicious-record-manipulation}), but proves
 * the same Protobuf binary masking pipeline through a real produce/fetch round trip. Mirrors
 * {@link RecordManipulationFilterAvroIT}, but for Protobuf instead of Avro.
 */
@ExtendWith(KafkaClusterExtension.class)
class RecordManipulationFilterProtobufIT extends BaseIT {

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

    private static byte[] originalValueBytes() {
        DynamicMessage message = DynamicMessage.newBuilder(DESCRIPTOR)
                .setField(DESCRIPTOR.findFieldByName("first_name"), "Harry")
                .setField(DESCRIPTOR.findFieldByName("age_years"), 17)
                .build();
        return new ProtobufBinarySerializer().serialize(message).array();
    }

    @Test
    void masksProtobufFieldOnProduceWhenDirectionIn(KafkaCluster cluster, Topic topic) {
        // Given
        var filterDef = protobufMaskingFilterDefinition("mask-in", topic, Direction.IN);
        var config = proxy(cluster)
                .addToFilterDefinitions(filterDef)
                .addToDefaultFilters(filterDef.name());

        try (var tester = kroxyliciousTester(config);
                var producer = tester.producer(Serdes.String(), Serdes.ByteArray(), Map.of())) {

            // When
            var sent = producer.send(new ProducerRecord<>(topic.name(), RECORD_KEY, originalValueBytes()));
            assertThat(sent).succeedsWithin(Duration.ofSeconds(5));

            // Then
            var records = consumeAll(tester, topic);
            assertThat(records)
                    .singleElement()
                    .satisfies(record -> {
                        assertThat(record.key()).isEqualTo(RECORD_KEY);
                        assertMasked(record);
                    });
        }
    }

    @Test
    void masksProtobufFieldOnFetchWhenDirectionOut(KafkaCluster cluster, Topic topic) {
        // Given
        var filterDef = protobufMaskingFilterDefinition("mask-out", topic, Direction.OUT);
        var config = proxy(cluster)
                .addToFilterDefinitions(filterDef)
                .addToDefaultFilters(filterDef.name());

        try (var tester = kroxyliciousTester(config);
                var producer = tester.producer(Serdes.String(), Serdes.ByteArray(), Map.of())) {

            // When
            var sent = producer.send(new ProducerRecord<>(topic.name(), RECORD_KEY, originalValueBytes()));
            assertThat(sent).succeedsWithin(Duration.ofSeconds(5));

            // Then
            var records = consumeAll(tester, topic);
            assertThat(records)
                    .singleElement()
                    .satisfies(this::assertMasked);
        }
    }

    @Test
    void passesThroughRecordsOnNonMatchingTopic(KafkaCluster cluster, Topic topic1, Topic topic2) {
        // Given
        var filterDef = protobufMaskingFilterDefinition("mask-topic1-only", topic1, Direction.IN);
        var config = proxy(cluster)
                .addToFilterDefinitions(filterDef)
                .addToDefaultFilters(filterDef.name());
        var originalValue = originalValueBytes();

        try (var tester = kroxyliciousTester(config);
                var producer = tester.producer(Serdes.String(), Serdes.ByteArray(), Map.of())) {

            // When
            var sent = producer.send(new ProducerRecord<>(topic2.name(), RECORD_KEY, originalValue));
            assertThat(sent).succeedsWithin(Duration.ofSeconds(5));

            // Then
            var records = consumeAll(tester, topic2);
            assertThat(records)
                    .singleElement()
                    .extracting(ConsumerRecord::value)
                    .isEqualTo(originalValue);
        }
    }

    private void assertMasked(ConsumerRecord<String, byte[]> record) {
        DynamicMessage maskedValue = new ProtobufBinaryDeserializer(DESCRIPTOR).deserialize(ByteBuffer.wrap(record.value()));
        assertThat(maskedValue.getField(DESCRIPTOR.findFieldByName("first_name")))
                .withFailMessage("expected first_name to have been masked")
                .isEqualTo("REDACTED");
        assertThat(maskedValue.getField(DESCRIPTOR.findFieldByName("age_years")))
                .withFailMessage("expected age_years to pass through unchanged (no apply chain configured for it)")
                .isEqualTo(17);
    }

    private static NamedFilterDefinition protobufMaskingFilterDefinition(String name, Topic topic, Direction direction) {
        Map<String, Object> recordTransform = Map.of(
                "intoRecordValue", Map.of(
                        "from", "RecordValue",
                        "apply", List.of(
                                Map.of("op", "DeserializeProtobuf", "protoText", SCHEMA_PROTO, "rootMessageName", ROOT_MESSAGE_NAME),
                                Map.of("op", "ProtobufTransform"),
                                Map.of("op", "SerializeProtobuf"))));

        String className = RecordManipulation.class.getName();
        return new NamedFilterDefinitionBuilder(name, className)
                .withConfig(Map.of(
                        "topic", topic.name(),
                        "direction", direction.name(),
                        "recordTransform", recordTransform))
                .build();
    }

    private ConsumerRecords<String, byte[]> consumeAll(KroxyliciousTester tester, Topic topic) {
        try (var consumer = tester.consumer(Serdes.String(), Serdes.ByteArray(),
                Map.of(GROUP_ID_CONFIG, UUID.randomUUID().toString(), AUTO_OFFSET_RESET_CONFIG, "earliest"))) {
            consumer.subscribe(Set.of(topic.name()));
            return consumer.poll(Duration.ofSeconds(10));
        }
    }

}

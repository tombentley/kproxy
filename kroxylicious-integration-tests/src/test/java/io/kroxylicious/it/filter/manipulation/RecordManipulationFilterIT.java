/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.kroxylicious.it.filter.manipulation;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.kroxylicious.filter.record.manipulation.filter.Direction;
import io.kroxylicious.filter.record.manipulation.filter.RecordManipulation;
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
 * {@code RecordManipulationFilterTest} (in {@code kroxylicious-record-manipulation}), but proves the
 * same JSON masking pipeline through a real produce/fetch round trip.
 */
@ExtendWith(KafkaClusterExtension.class)
class RecordManipulationFilterIT extends BaseIT {

    private static final String RECORD_KEY = "mykey";
    private static final String ORIGINAL_VALUE = """
            {"firstName":"Harry","ageYears":17}""";
    private static final ObjectMapper JSON = new ObjectMapper();

    @Test
    void masksJsonFieldOnProduceWhenDirectionIn(KafkaCluster cluster, Topic topic) {
        // Given
        var filterDef = jsonMaskingFilterDefinition("mask-in", topic, Direction.IN);
        var config = proxy(cluster)
                .addToFilterDefinitions(filterDef)
                .addToDefaultFilters(filterDef.name());

        try (var tester = kroxyliciousTester(config);
                var producer = tester.producer()) {

            // When
            var sent = producer.send(new ProducerRecord<>(topic.name(), RECORD_KEY, ORIGINAL_VALUE));
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
    void masksJsonFieldOnFetchWhenDirectionOut(KafkaCluster cluster, Topic topic) {
        // Given
        var filterDef = jsonMaskingFilterDefinition("mask-out", topic, Direction.OUT);
        var config = proxy(cluster)
                .addToFilterDefinitions(filterDef)
                .addToDefaultFilters(filterDef.name());

        try (var tester = kroxyliciousTester(config);
                var producer = tester.producer()) {

            // When
            var sent = producer.send(new ProducerRecord<>(topic.name(), RECORD_KEY, ORIGINAL_VALUE));
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
        var filterDef = jsonMaskingFilterDefinition("mask-topic1-only", topic1, Direction.IN);
        var config = proxy(cluster)
                .addToFilterDefinitions(filterDef)
                .addToDefaultFilters(filterDef.name());

        try (var tester = kroxyliciousTester(config);
                var producer = tester.producer()) {

            // When
            var sent = producer.send(new ProducerRecord<>(topic2.name(), RECORD_KEY, ORIGINAL_VALUE));
            assertThat(sent).succeedsWithin(Duration.ofSeconds(5));

            // Then
            var records = consumeAll(tester, topic2);
            assertThat(records)
                    .singleElement()
                    .extracting(ConsumerRecord::value)
                    .isEqualTo(ORIGINAL_VALUE);
        }
    }

    private void assertMasked(ConsumerRecord<String, String> record) {
        JsonNode maskedValue = readTree(record.value());
        assertThat(maskedValue.get("firstName").asText())
                .withFailMessage("expected firstName to have been masked")
                .isEqualTo("REDACTED");
        assertThat(maskedValue.get("ageYears").asInt())
                .withFailMessage("expected ageYears to pass through unchanged (no apply chain configured for it)")
                .isEqualTo(17);
    }

    private static JsonNode readTree(String value) {
        try {
            return JSON.readTree(value);
        }
        catch (Exception e) {
            throw new AssertionError("value was not valid JSON: " + value, e);
        }
    }

    private static NamedFilterDefinition jsonMaskingFilterDefinition(String name, Topic topic, Direction direction) {
        Map<String, Object> schema = Map.of(
                "type", "object",
                "properties", Map.of(
                        "firstName", Map.of(
                                "type", "string",
                                "apply", List.of(Map.of("op", "ValueString", "value", "REDACTED"))),
                        "ageYears", Map.of("type", "integer")));
        Map<String, Object> recordTransform = Map.of(
                "intoRecordValue", Map.of(
                        "from", "RecordValue",
                        "apply", List.of(
                                Map.of("op", "DeserializeJson"),
                                Map.of("op", "JsonTransform", "schema", schema),
                                Map.of("op", "SerializeJson"))));

        String className = RecordManipulation.class.getName();
        return new NamedFilterDefinitionBuilder(name, className)
                .withConfig(Map.of(
                        "topic", topic.name(),
                        "direction", direction.name(),
                        "recordTransform", recordTransform))
                .build();
    }

    private ConsumerRecords<String, String> consumeAll(KroxyliciousTester tester, Topic topic) {
        try (var consumer = tester.consumer(Map.of(GROUP_ID_CONFIG, UUID.randomUUID().toString(), AUTO_OFFSET_RESET_CONFIG, "earliest"))) {
            consumer.subscribe(Set.of(topic.name()));
            return consumer.poll(Duration.ofSeconds(10));
        }
    }

}

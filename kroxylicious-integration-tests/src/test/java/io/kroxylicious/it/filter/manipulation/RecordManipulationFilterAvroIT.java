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

import org.apache.avro.Schema;
import org.apache.avro.generic.GenericData;
import org.apache.avro.generic.GenericRecord;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.Serdes;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import io.kroxylicious.filter.record.manipulation.filter.Direction;
import io.kroxylicious.filter.record.manipulation.filter.RecordManipulation;
import io.kroxylicious.filter.record.manipulation.format.avro.AvroBinaryDeserializer;
import io.kroxylicious.filter.record.manipulation.format.avro.AvroBinarySerializer;
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
 * {@code RecordManipulationFilterAvroTest} (in {@code kroxylicious-record-manipulation}), but proves the
 * same Avro binary masking pipeline through a real produce/fetch round trip. Mirrors
 * {@link RecordManipulationFilterIT}, but for Avro instead of JSON.
 */
@ExtendWith(KafkaClusterExtension.class)
class RecordManipulationFilterAvroIT extends BaseIT {

    private static final String RECORD_KEY = "mykey";
    private static final String SCHEMA_JSON = """
            {"type": "record", "name": "User", "fields": [
                {"name": "firstName", "type": "string", "apply": [{"op": "ValueString", "value": "REDACTED"}]},
                {"name": "ageYears", "type": "int"}
            ]}
            """;
    private static final Schema SCHEMA = new Schema.Parser().parse(SCHEMA_JSON);

    private static byte[] originalValueBytes() {
        GenericRecord record = new GenericData.Record(SCHEMA);
        record.put("firstName", "Harry");
        record.put("ageYears", 17);
        return new AvroBinarySerializer(SCHEMA).serialize(record).array();
    }

    @Test
    void masksAvroFieldOnProduceWhenDirectionIn(KafkaCluster cluster, Topic topic) {
        // Given
        var filterDef = avroMaskingFilterDefinition("mask-in", topic, Direction.IN);
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
    void masksAvroFieldOnFetchWhenDirectionOut(KafkaCluster cluster, Topic topic) {
        // Given
        var filterDef = avroMaskingFilterDefinition("mask-out", topic, Direction.OUT);
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
        var filterDef = avroMaskingFilterDefinition("mask-topic1-only", topic1, Direction.IN);
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
        GenericRecord maskedValue = new AvroBinaryDeserializer(SCHEMA).deserialize(ByteBuffer.wrap(record.value()));
        assertThat(maskedValue.get("firstName").toString())
                .withFailMessage("expected firstName to have been masked")
                .isEqualTo("REDACTED");
        assertThat(maskedValue.get("ageYears"))
                .withFailMessage("expected ageYears to pass through unchanged (no apply chain configured for it)")
                .isEqualTo(17);
    }

    private static NamedFilterDefinition avroMaskingFilterDefinition(String name, Topic topic, Direction direction) {
        Map<String, Object> recordTransform = Map.of(
                "intoRecordValue", Map.of(
                        "from", "RecordValue",
                        "apply", List.of(
                                Map.of("op", "DeserializeAvro", "schema", SCHEMA_JSON),
                                Map.of("op", "AvroTransform", "schema", SCHEMA_JSON),
                                Map.of("op", "SerializeAvro", "schema", SCHEMA_JSON))));

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

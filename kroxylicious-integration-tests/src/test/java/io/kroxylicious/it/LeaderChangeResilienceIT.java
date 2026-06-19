/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.kroxylicious.it;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.kroxylicious.testing.kafka.api.TerminationStyle;

import static io.kroxylicious.testing.integration.tester.KroxyliciousTesters.kroxyliciousTester;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies that a proxied producer and consumer survive a leader change
 * on a backend cluster, and that all acknowledged sends are observable
 * by the consumer.
 */
class LeaderChangeResilienceIT extends TopicPartitionRoutingBaseIT {

    private static final Logger LOGGER = LoggerFactory.getLogger(LeaderChangeResilienceIT.class);

    @Test
    void shouldSurviveBrokerStopDuringProduce() throws Exception {
        String topic = "a.resilience-test";
        int replicationFactor = 3;
        int totalRecords = 50;
        int stopAfter = 20;

        // Given: a replicated topic on cluster-a
        try (var admin = AdminClient.create(clusterA.getKafkaClientConfiguration())) {
            admin.createTopics(List.of(new NewTopic(topic, Optional.of(1), Optional.of((short) replicationFactor))))
                    .all().get(10, TimeUnit.SECONDS);

            var descriptions = admin.describeTopics(List.of(topic)).allTopicNames().get(10, TimeUnit.SECONDS);
            int leaderNodeId = descriptions.get(topic).partitions().get(0).leader().id();
            LOGGER.info("Initial leader for {} is node {}", topic, leaderNodeId);

            var config = topicRouterConfig();

            // When: produce records via the proxy, stopping the leader mid-stream
            List<String> ackedValues = new ArrayList<>();
            try (var tester = kroxyliciousTester(config);
                    var producer = tester.producer(Map.of(
                            "acks", "all",
                            "retries", "10",
                            "retry.backoff.ms", "500",
                            "delivery.timeout.ms", "30000",
                            "request.timeout.ms", "10000"))) {

                for (int i = 0; i < totalRecords; i++) {
                    String value = "val-" + i;
                    Future<RecordMetadata> future = producer.send(new ProducerRecord<>(topic, "key", value));

                    if (i == stopAfter) {
                        LOGGER.info("Stopping broker node {} after {} records", leaderNodeId, stopAfter);
                        clusterA.stopNodes(nodeId -> nodeId == leaderNodeId, TerminationStyle.GRACEFUL);
                        LOGGER.info("Broker node {} stopped", leaderNodeId);
                    }

                    try {
                        future.get(15, TimeUnit.SECONDS);
                        ackedValues.add(value);
                    }
                    catch (Exception e) {
                        LOGGER.warn("Send failed for record {}: {}", i, e.getMessage());
                    }
                }
            }

            LOGGER.info("Acknowledged {} out of {} records", ackedValues.size(), totalRecords);

            // Then: all acknowledged sends are observable by direct consumption
            List<ConsumerRecord<String, String>> consumed = consumeDirectly(clusterA, topic);
            List<String> consumedValues = consumed.stream().map(ConsumerRecord::value).toList();

            assertThat(consumedValues)
                    .as("all acknowledged sends should be consumed")
                    .containsAll(ackedValues);

            assertThat(ackedValues)
                    .as("some records should have been acknowledged")
                    .isNotEmpty();

            // Restart broker for cleanup
            clusterA.startNodes(nodeId -> nodeId == leaderNodeId);
        }
    }
}

/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.proxy.encyption;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.TopicPartition;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import io.kroxylicious.proxy.config.ConfigurationBuilder;
import io.kroxylicious.proxy.config.FilterDefinitionBuilder;
import io.kroxylicious.testing.kafka.api.KafkaCluster;
import io.kroxylicious.testing.kafka.junit5ext.KafkaClusterExtension;

import static io.kroxylicious.test.tester.KroxyliciousConfigUtils.DEFAULT_VIRTUAL_CLUSTER;
import static io.kroxylicious.test.tester.KroxyliciousConfigUtils.proxy;
import static io.kroxylicious.test.tester.KroxyliciousTesters.kroxyliciousTester;
import static org.assertj.core.api.Assertions.assertThat;
import static org.testcontainers.shaded.org.awaitility.Awaitility.await;

@ExtendWith(KafkaClusterExtension.class)
class EnvelopeEncryptionFilterIT {

    @Test
    void roundTrip(KafkaCluster cluster) throws Exception {
        var builder = proxy(cluster);
        var key = UUID.randomUUID().toString();

        configure(builder, key);

        try (var tester = kroxyliciousTester(builder);
                var admin = tester.admin();
                var producer = tester.producer();
                var consumer = tester.consumer()) {

            String topic = tester.createTopic(DEFAULT_VIRTUAL_CLUSTER);

            await().atMost(Duration.ofSeconds(5)).until(() -> admin.listTopics().namesToListings().get(),
                    n -> n.containsKey(topic));

            var message = "hello world";
            producer.send(new ProducerRecord<>(topic, message)).get(5, TimeUnit.SECONDS);

            consumer.subscribe(List.of(topic));
            ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(10));
            assertThat(records).hasSize(1);
            assertThat(records.iterator()).toIterable().map(ConsumerRecord::value).containsExactly(message);
        }
    }

    @Test
    void topicRecordsAreUnreadableOnServer(KafkaCluster cluster, KafkaConsumer<String, String> clusterDirect) throws Exception {
        var builder = proxy(cluster);
        var key = UUID.randomUUID().toString();

        configure(builder, key);

        try (var tester = kroxyliciousTester(builder);
                var admin = tester.admin();
                var producer = tester.producer()) {

            String topic = tester.createTopic(DEFAULT_VIRTUAL_CLUSTER);

            await().atMost(Duration.ofSeconds(5)).until(() -> admin.listTopics().namesToListings().get(),
                    n -> n.containsKey(topic));

            var message = "hello world";
            producer.send(new ProducerRecord<>(topic, message)).get(5, TimeUnit.SECONDS);

            var tps = List.of(new TopicPartition(topic, 0));
            clusterDirect.assign(tps);
            clusterDirect.seekToBeginning(tps);
            ConsumerRecords<String, String> records = clusterDirect.poll(Duration.ofSeconds(10));
            assertThat(records.iterator()).toIterable()
                    .hasSize(1)
                    .map(ConsumerRecord::value).doesNotContain(message);
        }
    }

    /*
     *
     * further IT ideas:
     * verify that unencrypted messages are consumable
     * records with null values
     * fetching from > 1 topics (mixed encryption/plain case)
     * exploratory test examining what the client will see/do when decryption fails - looking to verify
     * - behaviour is reasonable
     * - the user has a chance to understand what's wrong.
     *
     */

    private void configure(ConfigurationBuilder builder, String key) {
        builder.addToFilters(new FilterDefinitionBuilder("EnvelopeEncryptionFilter")
                .withConfig("aliases", Map.of("all", key))
                .withConfig("keys", Map.of(key,
                        Map.of("key", "SEyeJwE78EvtCtRWpoFL3DN9JC/1wFR+XpNpJOPUt4E=", "algo", "AES")))
                .build());
    }

}

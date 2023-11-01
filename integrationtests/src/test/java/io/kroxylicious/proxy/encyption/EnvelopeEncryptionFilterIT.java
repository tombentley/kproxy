/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.proxy.encyption;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import io.kroxylicious.proxy.config.FilterDefinitionBuilder;
import io.kroxylicious.testing.kafka.api.KafkaCluster;
import io.kroxylicious.testing.kafka.junit5ext.KafkaClusterExtension;

import static io.kroxylicious.test.tester.KroxyliciousConfigUtils.DEFAULT_VIRTUAL_CLUSTER;
import static io.kroxylicious.test.tester.KroxyliciousConfigUtils.proxy;
import static io.kroxylicious.test.tester.KroxyliciousTesters.kroxyliciousTester;
import static org.assertj.core.api.Assertions.assertThat;
import static org.testcontainers.shaded.org.awaitility.Awaitility.await;

@ExtendWith(KafkaClusterExtension.class)
@Disabled("failing at fetch side")
class EnvelopeEncryptionFilterIT {

    @Test
    void roundTrip(KafkaCluster cluster) throws Exception {
        var builder = proxy(cluster);
        builder.addToFilters(new FilterDefinitionBuilder("EnvelopeEncryptionFilter").withConfig("foo", "bar").build());

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
            ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(5));
            assertThat(records).hasSize(1);
            assertThat(records.iterator()).toIterable().map(ConsumerRecord::value).containsExactly(message);
        }
    }

}
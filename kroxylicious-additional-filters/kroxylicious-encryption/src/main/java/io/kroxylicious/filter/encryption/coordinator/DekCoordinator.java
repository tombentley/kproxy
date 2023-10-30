/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.encryption.coordinator;

import java.time.Duration;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletionStage;

import io.kroxylicious.kms.service.De;
import io.kroxylicious.kms.service.Kms;

import io.kroxylicious.kms.service.Ser;

import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRebalanceListener;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.serialization.Serializer;
import org.apache.kafka.common.serialization.UUIDSerializer;
import org.apache.kafka.common.serialization.VoidDeserializer;

/**
 * Coordinates the generation of DEKs from a {@link Kms} and encapsulates policies on the usage of DEKs.
 * @param <K> The type of KEK reference
 * @param <E> The type of wrapped DEK
 */
public class DekCoordinator<K, E> {
    private final Consumer<Void, K> requestConsumer;

    private final Kms<K, E> dekGenerator;
    private final Producer<UUID, DekRecord<K, E>> dekProducer;
    private final String requestTopic;
    private final String responseTopic;

    public DekCoordinator(Consumer<Void, K> requestConsumer, String requestTopic,
                          Kms<K, E> dekGenerator,
                          Producer<UUID, DekRecord<K, E>> dekProducer, String responseTopic) {
        this.requestConsumer = requestConsumer;
        this.requestTopic = requestTopic;
        this.dekGenerator = dekGenerator;
        this.dekProducer = dekProducer;
        this.responseTopic = responseTopic;
    }

    <K, E> DekCoordinator build(Kms<K, E> dekGeneratorService,
                                String requestTopic,
                                String responseTopic) {
        Map<String, Object> consumerConfigs = null;
        var consumer = new KafkaConsumer<>(consumerConfigs, new VoidDeserializer(), De.toKafka(dekGeneratorService.keyRefDeserializer()));
        Serializer<UUID> uuidSerializer = new UUIDSerializer();
        Ser<E> edekSerializer = dekGeneratorService.edekSerializer();
        var kk = new Serializer<DekRecord<K, E>>() {

            @Override
            public byte[] serialize(String topic, DekRecord<K, E> data) {
                // TODO call the edekSerializer and add the extra stuff from the DekRecord
                return new byte[0];
            }
        };
        Map<String, Object> producerConfigs = null;
        var producer = new KafkaProducer<>(producerConfigs, uuidSerializer, kk);
        return new DekCoordinator<>(consumer, requestTopic, dekGeneratorService, producer, responseTopic);
    }

    public void run() {
        requestConsumer.subscribe(List.of(requestTopic), new ConsumerRebalanceListener() {
            @Override
            public void onPartitionsRevoked(Collection<TopicPartition> partitions) {

            }

            @Override
            public void onPartitionsAssigned(Collection<TopicPartition> partitions) {

            }
        });

        while (true) {
            var result = requestConsumer.poll(Duration.ofSeconds(10));
            for (var req : result) {
                K kek = req.value();
                handleRequest(kek).thenAccept(dekRecord -> {
                    dekProducer.send(new ProducerRecord<>(responseTopic, UUID.randomUUID(), dekRecord));
                });
            }
            requestConsumer.commitSync();
        }
    }

    public CompletionStage<DekRecord<K, E>> handleRequest(K kek) {
        var edekF = dekGenerator.generateDek(kek);
        return edekF.thenApply(edek -> {
            long creationTime = System.currentTimeMillis();
            long expiryTime = creationTime + 10_000;
            return new DekRecord<>(creationTime, expiryTime, kek, edek);
        });
    }

}

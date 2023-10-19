/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.encryption;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import io.kroxylicious.filter.encryption.coordinator.DekRecordSerializer;
import io.kroxylicious.kms.service.KmsService;

import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;

import io.kroxylicious.filter.encryption.coordinator.DekRecord;
import io.kroxylicious.kms.service.Kms;

import org.apache.kafka.common.serialization.Deserializer;
import org.apache.kafka.common.serialization.UUIDDeserializer;
import org.apache.kafka.common.serialization.VoidSerializer;

public class DekCache<K, E> {

    private final Kms<K, E> kms;

    private final ConcurrentHashMap<K, CompletableFuture<DekRecord<K, E>>> cache;

    private final Map<K, CompletableFuture<DekRecord<K, E>>> inFlight;

    private final Producer<Void, K> requestProducer;

    private final String requestTopic;

    private final Consumer<UUID, DekRecord<K, E>> consumer;

    private final String responseTopic;

    public DekCache(Kms<K, E> kms,
                    Producer<Void, K> requestProducer,
                    String requestTopic,
                    Consumer<UUID, DekRecord<K, E>> consumer,
                    String responseTopic) {
        this.kms = kms;
        this.requestProducer = requestProducer;
        this.requestTopic = requestTopic;
        this.cache = new ConcurrentHashMap<>();
        this.inFlight = new HashMap<>();
        this.consumer = consumer;
        this.responseTopic = responseTopic;
    }

    public static <K, E> DekCache<K, E> build(KmsService<Object, K, E> kmsService,
                                              String requestTopic,
                                              String responseTopic) {
        Kms<K, E> kms = kmsService.buildKms(null);
        Map<String, Object> producerConfig = null; // TODO
        var producer = new KafkaProducer<>(producerConfig, new VoidSerializer(), kmsService.keyRefSerializer());
        var edekDeserializer = kmsService.edekDeserializer();
        Deserializer<DekRecord<K, E>> dekRecordDeserializer = new DekRecordSerializer<>(edekDeserializer);
        Map<String, Object> consumerConfigs = null; // TODO
        KafkaConsumer<UUID, DekRecord<K, E>> consumer = new KafkaConsumer<>(consumerConfigs,
                new UUIDDeserializer(),
                dekRecordDeserializer);
        return new DekCache<>(kms, producer, requestTopic, consumer, responseTopic);
    }

    public CompletableFuture<Encryptor> encryptor(K kek) {
        return cache.compute(kek, (k, cf) -> {
            if (cf == null) {
                // a newly observed kek
                return newDek(kek);
            } else if (!cf.isDone()) {
                // inflight
                return cf;
            } else if (cf.isCompletedExceptionally()) {
                // TODO
                throw  new IllegalStateException();
            } else {
                DekRecord<K, E> dek;
                try {
                    dek = cf.get();
                }
                catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
                catch (ExecutionException e) {
                    throw new RuntimeException(e);
                }
                if (!isValid(dek)) {
                    return newDek(kek);
                }
                return cf;
            }
        }).thenCompose(dek ->
                // make async the request to the KMS to decode the edek
                kms.decryptEdek(dek.kek(), dek.edek())
        ).thenApply(sk -> {
                // TODO CF needs to include the dekId
                // TODO serialization needs to add the UUID (even if encryptor is unaware)
                // TODO decide whether encryptor is separate from serialization
                // TODO think about deser -- needs to get get the dekId
                //      separately from the ciphertext (or use a view??)
                UUID dekId = null;
                return new Encryptor(4, 16, rng, sk);
        });
    }
    
    private CompletableFuture<DekRecord<K, E>> newDek(K kek) {
        CompletableFuture<DekRecord<K, E>> dekCompletableFuture = new CompletableFuture<>();
        inFlight.put(kek, dekCompletableFuture);

        requestProducer.send(new ProducerRecord<>(requestTopic, null, kek));
        return dekCompletableFuture;
    }

    public void run() {
        consumer.subscribe(List.of(responseTopic));
        while (true) {
            var results = consumer.poll(Duration.ofSeconds(10_000));
            for (var r : results) {
                var dekRecord = r.value();
                var cf = inFlight.get(dekRecord.kek());
                cf.complete(dekRecord);
            }
        }
    }

    private boolean isValid(DekRecord<K, E> v) {
        return System.currentTimeMillis() < v.notBefore()
                && v.notAfter() < System.currentTimeMillis();
    }

    public CompletableFuture<Map<String, Encryptor>> encryptors(Map<String, K> keks) {
        var kekArray = keks.entrySet().stream().map(entry -> encryptor(entry.getValue()).thenApply(enc -> {
            return Map.entry(entry.getKey(), enc);
        })).toList();
        return CompletableFuture.allOf(kekArray.toArray((CompletableFuture<Map.Entry<String, Encryptor>>[]) new CompletableFuture[0])).thenApply(ignored -> {
            var entryStream = kekArray.stream()
                    .map(entryCompletableFuture -> {
                        try {
                            return entryCompletableFuture.get();
                        }
                        catch (InterruptedException | ExecutionException e) {
                            throw new RuntimeException(e);
                        }

                    });
            return entryStream.collect(Collectors.toMap(
                    Map.Entry::getKey,
                    Map.Entry::getValue));
        });
    }
}

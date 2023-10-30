/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.encryption.coordinator;

import java.nio.ByteBuffer;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import io.kroxylicious.filter.encryption.AesGcmEncryptor;
import io.kroxylicious.filter.encryption.AesGcmIvGenerator;
import io.kroxylicious.filter.encryption.DekCache;
import io.kroxylicious.filter.encryption.DekContext;
import io.kroxylicious.kms.service.Ser;

import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.Deserializer;
import org.apache.kafka.common.serialization.UUIDDeserializer;
import org.apache.kafka.common.serialization.VoidSerializer;

import io.kroxylicious.kms.service.Kms;

public class CoordinatedDekCache<K, E> /*implements DekCache<K, E>*/ {

    private final Kms<K, E> kms;

    private final ConcurrentHashMap<K, CompletableFuture<Map.Entry<UUID, DekRecord<K, E>>>> cache;

    private final Map<K, CompletableFuture<Map.Entry<UUID, DekRecord<K, E>>>> inFlight;

    private final Producer<Void, K> requestProducer;

    private final String requestTopic;

    private final Consumer<UUID, DekRecord<K, E>> consumer;

    private final String responseTopic;

    public CoordinatedDekCache(Kms<K, E> kms,
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

    public static <K, E> DekCache<K, UUID> build(Kms<K, E> kms,
                                                         String requestTopic,
                                                         String responseTopic) {
        Map<String, Object> producerConfig = null; // TODO
        var producer = new KafkaProducer<>(producerConfig, new VoidSerializer(), kms.keyRefSerializer());
        var edekDeserializer = kms.edekDeserializer();
        Deserializer<DekRecord<K, E>> dekRecordDeserializer = new DekRecordSerializer<>(edekDeserializer);
        Map<String, Object> consumerConfigs = null; // TODO
        KafkaConsumer<UUID, DekRecord<K, E>> consumer = new KafkaConsumer<>(consumerConfigs,
                new UUIDDeserializer(),
                dekRecordDeserializer);
        return new CoordinatedDekCache<>(kms, producer, requestTopic, consumer, responseTopic);
    }

    private CompletableFuture<AesGcmEncryptor> encryptor(K kek) {
        return cache.compute(kek, (k, cf) -> {
            if (cf == null) {
                // a newly observed kek
                return newDek(kek);
            }
            else if (!cf.isDone()) {
                // inflight
                return cf;
            }
            else if (cf.isCompletedExceptionally()) {
                // TODO
                throw new IllegalStateException();
            }
            else {
                Map.Entry<UUID, DekRecord<K, E>> dek;
                try {
                    dek = cf.get();
                }
                catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
                catch (ExecutionException e) {
                    throw new RuntimeException(e);
                }
                if (!isValid(dek.getValue())) {
                    return newDek(kek);
                }
                return cf;
            }
        }).thenCompose(pair ->
            // make async the request to the KMS to decode the edek
            kms.decryptEdek(pair.getValue().kek(), pair.getValue().edek()).thenApply(xx -> Map.entry(pair.getKey(), xx)))
        .thenApply(pair -> {
            // TODO CF needs to include the dekId
            // TODO serialization needs to add the UUID (even if encryptor is unaware)
            // TODO decide whether encryptor is separate from serialization
            // TODO think about deser -- needs to get get the dekId
            // separately from the ciphertext (or use a view??)

            return new AesGcmEncryptor(new AesGcmIvGenerator(new SecureRandom()), pair.getValue()));
        });
    }

    private CompletableFuture<Map.Entry<UUID, DekRecord<K, E>>> newDek(K kek) {
        CompletableFuture<Map.Entry<UUID, DekRecord<K, E>>> dekCompletableFuture = new CompletableFuture<>();
        inFlight.put(kek, dekCompletableFuture);

        requestProducer.send(new ProducerRecord<>(requestTopic, null, kek));
        return dekCompletableFuture;
    }

    public void run() {
        consumer.subscribe(List.of(responseTopic));
        while (true) {
            var results = consumer.poll(Duration.ofSeconds(10_000));
            for (var r : results) {
                var uuid = r.key();
                var dekRecord = r.value();
                var cf = inFlight.get(dekRecord.kek());
                cf.complete(Map.entry(uuid, dekRecord));
            }
        }
    }

    private boolean isValid(DekRecord<K, E> v) {
        return System.currentTimeMillis() < v.notBefore()
                && v.notAfter() < System.currentTimeMillis();
    }

    //@Override
    public Ser<UUID> serializer() {
        return new Ser<>() {
            @Override
            public int sizeOf(UUID uuid) {
                return 16;
            }

            @Override
            public void serialize(UUID uuid, ByteBuffer buffer) {
                buffer.putLong(uuid.getMostSignificantBits());
                buffer.putLong(uuid.getLeastSignificantBits());
            }
        };
    }

    //@Override
    public CompletableFuture<Map<String, DekContext<K>>> encryptors(Map<String, K> keks) {
        var kekArray = keks.entrySet().stream()
                .map(entry -> encryptor(entry.getValue()).thenApply(enc -> {
            return Map.entry(entry.getKey(), enc);
        })).toList();
        return CompletableFuture.allOf(kekArray.toArray((CompletableFuture<Map.Entry<String, Map.Entry<UUID, AesGcmEncryptor>>>[]) new CompletableFuture[0])).thenApply(ignored -> {
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
                    e -> e.getKey(),
                    e -> e.getValue()));
        });
    }
}

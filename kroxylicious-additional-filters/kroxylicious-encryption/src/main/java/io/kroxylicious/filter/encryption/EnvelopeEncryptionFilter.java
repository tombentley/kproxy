/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.encryption;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import org.apache.kafka.common.message.FetchResponseData;
import org.apache.kafka.common.message.FetchResponseData.FetchableTopicResponse;
import org.apache.kafka.common.message.FetchResponseData.PartitionData;
import org.apache.kafka.common.message.ProduceRequestData;
import org.apache.kafka.common.message.ProduceRequestData.PartitionProduceData;
import org.apache.kafka.common.message.ProduceRequestData.TopicProduceData;
import org.apache.kafka.common.message.RequestHeaderData;
import org.apache.kafka.common.message.ResponseHeaderData;
import org.apache.kafka.common.record.MemoryRecords;
import org.apache.kafka.common.record.MemoryRecordsBuilder;
import org.apache.kafka.common.record.RecordBatch;

import io.kroxylicious.proxy.filter.FetchResponseFilter;
import io.kroxylicious.proxy.filter.FilterContext;
import io.kroxylicious.proxy.filter.ProduceRequestFilter;
import io.kroxylicious.proxy.filter.RequestFilterResult;
import io.kroxylicious.proxy.filter.ResponseFilterResult;

/**
 * A filter for encrypting and decrypting records using envelope encryption
 * @param <K> The type of KEK reference
 * @param <E> The type of wrapped DEK
 */
class EnvelopeEncryptionFilter<K, E>
        implements ProduceRequestFilter, FetchResponseFilter {
    private final TopicNameBasedKekSelector<K> kekSelector;

    private final DekCache<K, E> dekCache;

    private BufferPool bufferPool;

    EnvelopeEncryptionFilter(DekCache<K, E> dekCache, TopicNameBasedKekSelector<K> kekSelector) {
        this.kekSelector = kekSelector;
        this.dekCache = dekCache;
    }

    @SuppressWarnings("unchecked")
    private static <T> CompletableFuture<List<T>> join(List<CompletableFuture<T>> futures) {
        return CompletableFuture.allOf(futures.toArray((CompletableFuture<T>[]) new CompletableFuture[futures.size()])).thenApply(ignored -> {
            return futures.stream().map(cf -> {
                try {
                    return cf.get();
                }
                catch (InterruptedException | ExecutionException e) {
                    // this should be impossible, because if any of the futures completed exceptionally then the thenApply()
                    // won't be called
                    throw new IllegalStateException(e);
                }

            }).toList();
        });
    }

    private CompletableFuture<Map<String, DekContext<K>>> keyContexts(Map<String, K> topicToKekId) {
        Map<K, List<String>> inverted = new HashMap<>();
        Set<K> kekIds = new HashSet<>(topicToKekId.size());
        topicToKekId.forEach((topicName, kekId) -> {
            kekIds.add(kekId);
            inverted.computeIfAbsent(kekId, k -> new ArrayList<>()).add(topicName);
        });
        var futures = kekIds.stream().map(kekId -> dekCache.forKekId(kekId).toCompletableFuture()).toList();

        return join(futures).thenApply(list -> {
            Map<String, DekContext<K>> result = new HashMap<>(topicToKekId.size());
            list.forEach(dekContext -> inverted.get(dekContext.kekId()).forEach(topicName -> result.put(topicName, dekContext)));
            return result;
        });
    }

    @Override
    public CompletionStage<RequestFilterResult> onProduceRequest(short apiVersion, RequestHeaderData header, ProduceRequestData request, FilterContext context) {
        var topicNames = request.topicData().stream().map(TopicProduceData::name).collect(Collectors.toSet());
        return kekSelector.selectKek(topicNames)
                .thenCompose(kekMap -> keyContexts(kekMap).thenApply(topicNameToKeyContext -> {
                    request.topicData().forEach(td -> {
                        var keyContext = topicNameToKeyContext.get(td.name());
                        if (keyContext != null) {
                            td.setPartitionData(encryptPartition(keyContext, td.partitionData()));
                        }
                    });
                    return request;
                }).thenCompose(yy -> context.forwardRequest(header, request)));
    }

    private List<PartitionProduceData> encryptPartition(DekContext<K> dekContext,
                                                        List<PartitionProduceData> oldPartitionData) {
        List<PartitionProduceData> newPartitionData = new ArrayList<>(oldPartitionData.size());
        for (var pd : oldPartitionData) {
            int partitionId = pd.index();
            ByteBuffer buffer = null; /// TODO determine how we should allocate this (is it a configuration parameter??)
            MemoryRecords records = (MemoryRecords) pd.records();
            MemoryRecordsBuilder builder = recordsBuilder(buffer, records);
            for (var kafkaRecord : records.records()) {
                ByteBuffer output = null;
                try {
                    output = bufferPool.acquire(dekContext.encodedSize(kafkaRecord.valueSize()));
                    dekContext.encode(kafkaRecord.value(), output);
                    builder.append(kafkaRecord.timestamp(), kafkaRecord.key(), output, kafkaRecord.headers());
                }
                finally {
                    if (output != null) {
                        bufferPool.release(output);
                    }
                }
            }
            newPartitionData.add(new PartitionProduceData()
                    .setIndex(partitionId)
                    .setRecords(builder.build()));
        }
        return newPartitionData;
    }

    @Override
    public CompletionStage<ResponseFilterResult> onFetchResponse(short apiVersion, ResponseHeaderData header, FetchResponseData response, FilterContext context) {
        return maybeDecodeFetch(response.responses())
                .thenCompose(responses -> context.forwardResponse(header, new FetchResponseData().setResponses(responses)));
    }

    private CompletableFuture<List<FetchableTopicResponse>> maybeDecodeFetch(List<FetchableTopicResponse> topics) {
        List<CompletableFuture<FetchableTopicResponse>> result = new ArrayList<>();
        for (FetchableTopicResponse topicData : topics) {
            result.add(maybeDecodePartitions(topicData.partitions()).thenApply(kk -> {
                topicData.setPartitions(kk);
                return topicData;
            }));
        }
        return join(result);
    }

    private CompletableFuture<List<PartitionData>> maybeDecodePartitions(List<PartitionData> partitions) {
        List<CompletableFuture<PartitionData>> result = new ArrayList<>();
        for (PartitionData partitionData : partitions) {
            if (!(partitionData.records() instanceof MemoryRecords)) {
                throw new IllegalStateException();
            }
            result.add(maybeDecodeRecords(partitionData, (MemoryRecords) partitionData.records()));
        }
        return join(result);
    }

    private CompletableFuture<PartitionData> maybeDecodeRecords(PartitionData fpr, MemoryRecords memoryRecords) {
        final CompletableFuture<PartitionData> result;
        if (!anyRecordSmellsOfEncryption(memoryRecords)) {
            result = CompletableFuture.completedFuture(fpr);
        }
        else {
            ByteBuffer buffer = null; // TODO
            MemoryRecordsBuilder builder = recordsBuilder(buffer, memoryRecords);
            List<CompletableFuture<Integer>> futures = new ArrayList<>();
            for (var kafkaRecord : memoryRecords.records()) {
                if (!smellsOfEncryption(kafkaRecord.value())) {
                    builder.append(kafkaRecord.timestamp(), kafkaRecord.key(), kafkaRecord.value(), kafkaRecord.headers());
                    futures.add(CompletableFuture.completedFuture(1));
                }
                else {
                    ByteBuffer value = kafkaRecord.value();
                    var cf = dekCache.resolve(value).thenApply((DekContext<K> dekContext) -> {

                        ByteBuffer output = null;
                        try {
                            int plaintextSize = kafkaRecord.valueSize();
                            // TODO ^^ this is an overestimate!
                            output = bufferPool.acquire(plaintextSize);
                            dekContext.decode(value, output);
                            builder.append(kafkaRecord.timestamp(), kafkaRecord.key(), output, kafkaRecord.headers());
                            return 1;
                        }
                        finally {
                            bufferPool.release(output);
                        }
                    }).toCompletableFuture();
                    futures.add(cf);
                }
            }
            result = join(futures)
                    .thenApply(ignored -> builder.build())
                    .thenApply(fpr::setRecords);
        }
        return result;
    }

    private boolean anyRecordSmellsOfEncryption(MemoryRecords memoryRecords) {
        return StreamSupport.stream(memoryRecords.records().spliterator(), false)
                .anyMatch(kafkaRecord -> smellsOfEncryption(kafkaRecord.value()));
    }

    private boolean smellsOfEncryption(ByteBuffer value) {
        return true; // TODO implement this
    }

    private static MemoryRecordsBuilder recordsBuilder(ByteBuffer buffer, MemoryRecords records) {
        RecordBatch firstBatch = records.firstBatch();
        return new MemoryRecordsBuilder(buffer,
                firstBatch.magic(),
                firstBatch.compressionType(), // TODO we might not want to use the client's compression
                firstBatch.timestampType(),
                firstBatch.baseOffset(),
                0L,
                firstBatch.producerId(),
                firstBatch.producerEpoch(),
                firstBatch.baseSequence(),
                firstBatch.isTransactional(),
                firstBatch.isControlBatch(),
                firstBatch.partitionLeaderEpoch(),
                0);
    }
}

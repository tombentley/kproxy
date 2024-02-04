/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.encryption;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.kafka.common.message.FetchResponseData;
import org.apache.kafka.common.message.FetchResponseData.FetchableTopicResponse;
import org.apache.kafka.common.message.FetchResponseData.PartitionData;
import org.apache.kafka.common.message.ProduceRequestData;
import org.apache.kafka.common.message.ProduceRequestData.TopicProduceData;
import org.apache.kafka.common.message.RequestHeaderData;
import org.apache.kafka.common.message.ResponseHeaderData;
import org.apache.kafka.common.record.MemoryRecords;
import org.slf4j.Logger;

import io.kroxylicious.proxy.filter.FetchResponseFilter;
import io.kroxylicious.proxy.filter.FilterContext;
import io.kroxylicious.proxy.filter.ProduceRequestFilter;
import io.kroxylicious.proxy.filter.RequestFilterResult;
import io.kroxylicious.proxy.filter.ResponseFilterResult;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * A filter for encrypting and decrypting records using envelope encryption
 * @param <K> The type of KEK reference
 */
public class EnvelopeEncryptionFilter<K>
        implements ProduceRequestFilter, FetchResponseFilter {
    private static final Logger log = getLogger(EnvelopeEncryptionFilter.class);
    private final TopicNameBasedKekSelector<K> kekSelector;

    private final EncryptionManager<K> encryptionManager;
    private final DecryptionManager decryptionManager;

    EnvelopeEncryptionFilter(EncryptionManager<K> encryptionManager,
                             DecryptionManager decryptionManager, TopicNameBasedKekSelector<K> kekSelector) {
        this.kekSelector = kekSelector;
        this.encryptionManager = encryptionManager;
        this.decryptionManager = decryptionManager;
    }

    @SuppressWarnings("unchecked")
    public static <T> CompletionStage<List<T>> join(List<? extends CompletionStage<T>> stages) {
        CompletableFuture<T>[] futures = stages.stream().map(CompletionStage::toCompletableFuture).toArray(CompletableFuture[]::new);
        return CompletableFuture.allOf(futures)
                .thenApply(ignored -> Stream.of(futures).map(CompletableFuture::join).toList());
    }

    @Override
    public CompletionStage<RequestFilterResult> onProduceRequest(short apiVersion,
                                                                 RequestHeaderData header,
                                                                 ProduceRequestData request,
                                                                 FilterContext context) {
        return maybeEncodeProduce(request, context)
                .thenCompose(yy -> context.forwardRequest(header, request));
    }

    private CompletionStage<ProduceRequestData> maybeEncodeProduce(ProduceRequestData request, FilterContext context) {
        var topicNameToData = request.topicData().stream().collect(Collectors.toMap(TopicProduceData::name, Function.identity()));
        return kekSelector.selectKek(topicNameToData.keySet()) // figure out what keks we need
                .thenCompose(kekMap -> {
                    var futures = kekMap.entrySet().stream().flatMap(e -> {
                        String topicName = e.getKey();
                        var kekId = e.getValue();
                        TopicProduceData tpd = topicNameToData.get(topicName);
                        return tpd.partitionData().stream().map(ppd -> {
                            // handle case where this topic is to be left unencrypted
                            if (kekId == null) {
                                return CompletableFuture.completedStage(ppd);
                            }
                            MemoryRecords records = (MemoryRecords) ppd.records();
                            return encryptionManager.encrypt(
                                    topicName,
                                    ppd.index(),
                                    new EncryptionScheme<>(kekId, EnumSet.of(RecordField.RECORD_VALUE)),
                                    records,
                                    context::createByteBufferOutputStream)
                                    .thenApply(ppd::setRecords);
                        });
                    }).toList();
                    return join(futures).thenApply(x -> request);
                }).exceptionallyCompose(throwable -> {
                    log.warn("failed to encrypt records", throwable);
                    return CompletableFuture.failedStage(throwable);
                });
    }

    @Override
    public CompletionStage<ResponseFilterResult> onFetchResponse(short apiVersion, ResponseHeaderData header, FetchResponseData response, FilterContext context) {
        return maybeDecodeFetch(response.responses(), context)
                .thenCompose(responses -> context.forwardResponse(header, response.setResponses(responses)))
                .exceptionallyCompose(throwable -> {
                    log.warn("failed to decrypt records", throwable);
                    return CompletableFuture.failedStage(throwable);
                });
    }

    private CompletionStage<List<FetchableTopicResponse>> maybeDecodeFetch(List<FetchableTopicResponse> topics, FilterContext context) {
        List<CompletionStage<FetchableTopicResponse>> result = new ArrayList<>(topics.size());
        for (FetchableTopicResponse topicData : topics) {
            result.add(maybeDecodePartitions(topicData.topic(), topicData.partitions(), context).thenApply(kk -> {
                topicData.setPartitions(kk);
                return topicData;
            }));
        }
        return join(result);
    }

    private CompletionStage<List<PartitionData>> maybeDecodePartitions(String topicName,
                                                                       List<PartitionData> partitions,
                                                                       FilterContext context) {
        List<CompletionStage<PartitionData>> result = new ArrayList<>(partitions.size());
        for (PartitionData partitionData : partitions) {
            if (!(partitionData.records() instanceof MemoryRecords)) {
                throw new IllegalStateException();
            }
            result.add(maybeDecodeRecords(topicName, partitionData, (MemoryRecords) partitionData.records(), context));
        }
        return join(result);
    }

    private CompletionStage<PartitionData> maybeDecodeRecords(String topicName,
                                                              PartitionData fpr,
                                                              MemoryRecords memoryRecords,
                                                              FilterContext context) {
        return decryptionManager.decrypt(
                topicName,
                fpr.partitionIndex(),
                memoryRecords,
                context::createByteBufferOutputStream)
                .thenApply(fpr::setRecords);
    }

}

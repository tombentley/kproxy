/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.encryption;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletionStage;
import java.util.stream.Collectors;

import org.apache.kafka.common.message.FetchResponseData;
import org.apache.kafka.common.message.ProduceRequestData;
import org.apache.kafka.common.message.ProduceRequestData.TopicProduceData;
import org.apache.kafka.common.message.RequestHeaderData;
import org.apache.kafka.common.message.ResponseHeaderData;
import org.apache.kafka.common.record.MemoryRecords;
import org.apache.kafka.common.record.MemoryRecordsBuilder;
import org.apache.kafka.common.record.RecordBatch;

import io.kroxylicious.kms.service.KmsService;
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
public class EnvelopeEncryptionFilter<K, E>
        implements ProduceRequestFilter, FetchResponseFilter {
    private final TopicNameBasedKekSelector<K> kekSelector;

    private final DekCache<K, E> dekCache;

    public EnvelopeEncryptionFilter(KmsService<Object, K, E> kms, TopicNameBasedKekSelector<K> kekSelector) {
        this.kekSelector = kekSelector;
        this.dekCache = DekCache.build(kms, "", "");
    }

    @Override
    public CompletionStage<RequestFilterResult> onProduceRequest(short apiVersion, RequestHeaderData header, ProduceRequestData request, FilterContext context) {
        var topicNames = request.topicData().stream().map(TopicProduceData::name).collect(Collectors.toSet());
        return kekSelector.selectKek(topicNames)
                .thenCompose(kekMap -> dekCache.encryptors(kekMap).thenApply(topicNameToEncryptor -> {
                    request.topicData().forEach(td -> {
                        var encryptor = topicNameToEncryptor.get(td.name());
                        if (encryptor != null) {
                            td.setPartitionData(encryptPartition(encryptor, td.partitionData()));
                        }
                    });
                    return request;
                }).thenCompose(yy -> context.forwardRequest(header, request)));
    }

    private List<ProduceRequestData.PartitionProduceData> encryptPartition(UUID dekRef, Encryptor encryptor,
                                                                           List<ProduceRequestData.PartitionProduceData> oldPartitionData) {
        List<ProduceRequestData.PartitionProduceData> newPartitionData = new ArrayList<>(oldPartitionData.size());
        for (var pd : oldPartitionData) {
            int partitionId = pd.index();
            ByteBuffer buffer = null;
            MemoryRecords records = (MemoryRecords) pd.records();
            MemoryRecordsBuilder builder = recordsBuilder(buffer, records);
            for (var kafkaRecord : records.records()) {
                ByteBuffer ciphertext = null; // TODO figure out size of ciphertext
                ciphertext.put(version);
                ciphertext.put(dekRef);
                encryptor.encrypt(kafkaRecord.value(), ciphertext);
                builder.append(kafkaRecord.timestamp(), kafkaRecord.key(), ciphertext, kafkaRecord.headers());
            }
            newPartitionData.add(new ProduceRequestData.PartitionProduceData()
                    .setIndex(partitionId)
                    .setRecords(builder.build()));
        }
        return newPartitionData;
    }

    private static MemoryRecordsBuilder recordsBuilder(ByteBuffer buffer, MemoryRecords records) {
        RecordBatch firstBatch = records.firstBatch();
        return new MemoryRecordsBuilder(buffer,
                firstBatch.magic(),
                firstBatch.compressionType(),
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

    @Override
    public CompletionStage<ResponseFilterResult> onFetchResponse(short apiVersion, ResponseHeaderData header, FetchResponseData response, FilterContext context) {
        return null;
    }

}

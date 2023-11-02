/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.encryption;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.apache.kafka.common.message.FetchResponseData;
import org.apache.kafka.common.message.FetchResponseData.FetchableTopicResponse;
import org.apache.kafka.common.message.FetchResponseData.PartitionData;
import org.apache.kafka.common.message.ProduceRequestData;
import org.apache.kafka.common.message.ProduceRequestData.TopicProduceData;
import org.apache.kafka.common.message.RequestHeaderData;
import org.apache.kafka.common.message.ResponseHeaderData;
import org.apache.kafka.common.record.MemoryRecords;
import org.apache.kafka.common.record.MemoryRecordsBuilder;
import org.apache.kafka.common.record.RecordBatch;
import org.apache.kafka.common.utils.ByteBufferOutputStream;

import io.kroxylicious.proxy.filter.FetchResponseFilter;
import io.kroxylicious.proxy.filter.FilterContext;
import io.kroxylicious.proxy.filter.ProduceRequestFilter;
import io.kroxylicious.proxy.filter.RequestFilterResult;
import io.kroxylicious.proxy.filter.ResponseFilterResult;

import edu.umd.cs.findbugs.annotations.NonNull;

/**
 * A filter for encrypting and decrypting records using envelope encryption
 * @param <K> The type of KEK reference
 */
class EnvelopeEncryptionFilter<K>
        implements ProduceRequestFilter, FetchResponseFilter {
    private final TopicNameBasedKekSelector<K> kekSelector;

    private final KeyManager<K> keyManager;

    EnvelopeEncryptionFilter(KeyManager<K> keyManager, TopicNameBasedKekSelector<K> kekSelector) {
        this.kekSelector = kekSelector;
        this.keyManager = keyManager;
    }

    @SuppressWarnings("unchecked")
    static <T> CompletionStage<List<T>> join(List<? extends CompletionStage<T>> stages) {
        CompletableFuture<T>[] futures = new CompletableFuture[stages.size()];
        for (int i = 0; i < stages.size(); i++) {
            futures[i] = stages.get(i).toCompletableFuture();
        }
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
                            var buffer = context.createByteBufferOutputStream(0);
                            // ^^ TODO determine how we should allocate this (is it a configuration parameter??)
                            MemoryRecords records = (MemoryRecords) ppd.records();
                            MemoryRecordsBuilder builder = recordsBuilder(buffer, records);
                            var recordStream = recordStream(records);
                            var encryptionRequests = recordStream.map(r -> {
                                return new RecordEncryptionRequest(r, r.valueSize(), r.value());
                            });
                            return keyManager.encrypt(
                                    kekId,
                                    encryptionRequests,
                                    (encryptedBuffer, kafkaRecord) -> {
                                        builder.append(kafkaRecord.timestamp(), kafkaRecord.key(), encryptedBuffer, kafkaRecord.headers());
                                    })
                                    .thenApply(ignored -> ppd.setRecords(builder.build()));
                        });
                    }).toList();
                    return join(futures).thenApply(x -> request);
                });
    }

    @Override
    public CompletionStage<ResponseFilterResult> onFetchResponse(short apiVersion, ResponseHeaderData header, FetchResponseData response, FilterContext context) {
        return maybeDecodeFetch(response.responses(), context)
                .thenCompose(responses -> context.forwardResponse(header, response.setResponses(responses)));
    }

    private CompletionStage<List<FetchableTopicResponse>> maybeDecodeFetch(List<FetchableTopicResponse> topics, FilterContext context) {
        List<CompletionStage<FetchableTopicResponse>> result = new ArrayList<>();
        for (FetchableTopicResponse topicData : topics) {
            result.add(maybeDecodePartitions(topicData.partitions(), context).thenApply(kk -> {
                topicData.setPartitions(kk);
                return topicData;
            }));
        }
        return join(result);
    }

    private CompletionStage<List<PartitionData>> maybeDecodePartitions(List<PartitionData> partitions, FilterContext context) {
        List<CompletionStage<PartitionData>> result = new ArrayList<>();
        for (PartitionData partitionData : partitions) {
            if (!(partitionData.records() instanceof MemoryRecords)) {
                throw new IllegalStateException();
            }
            result.add(maybeDecodeRecords(partitionData, (MemoryRecords) partitionData.records(), context));
        }
        return join(result);
    }

    private CompletionStage<PartitionData> maybeDecodeRecords(PartitionData fpr,
                                                              MemoryRecords memoryRecords,
                                                              FilterContext context) {
        final CompletionStage<PartitionData> result;
        if (!anyRecordSmellsOfEncryption(memoryRecords)) {
            result = CompletableFuture.completedFuture(fpr);
        }
        else {
            var buffer = context.createByteBufferOutputStream(0); // TODO
            MemoryRecordsBuilder builder = recordsBuilder(buffer, memoryRecords);
            var futures = recordStream(memoryRecords)
                    .map(kafkaRecord -> maybeDecryptRecord(builder, kafkaRecord))
                    .toList();
            result = join(futures)
                    .thenApply(ignored -> builder.build())
                    .thenApply(fpr::setRecords);
        }
        return result;
    }

    @NonNull
    private static Stream<org.apache.kafka.common.record.Record> recordStream(MemoryRecords memoryRecords) {
        return StreamSupport.stream(memoryRecords.records().spliterator(), false);
    }

    @NonNull
    private CompletionStage<Void> maybeDecryptRecord(MemoryRecordsBuilder builder, org.apache.kafka.common.record.Record kafkaRecord) {
        if (!smellsOfEncryption(kafkaRecord.value())) {
            builder.append(kafkaRecord.timestamp(), kafkaRecord.key(), kafkaRecord.value(), kafkaRecord.headers());
            return CompletableFuture.completedFuture(null);
        }
        else {
            return keyManager.decrypt(
                    kafkaRecord,
                    (plaintextBuffer, kafkaRecord2) -> {
                        // .. and use it to encrypt the records in the partitions for those topics
                        // TODO this receiver pattern doesn't compose nicely, but it does avoid allocating a record for each record.
                        builder.append(kafkaRecord2.timestamp(), kafkaRecord2.key(), plaintextBuffer, kafkaRecord2.headers());
                    });
        }
    }

    private boolean anyRecordSmellsOfEncryption(MemoryRecords memoryRecords) {
        return recordStream(memoryRecords)
                .anyMatch(kafkaRecord -> smellsOfEncryption(kafkaRecord.value()));
    }

    private boolean smellsOfEncryption(ByteBuffer value) {
        return true; // TODO implement this
    }

    private static MemoryRecordsBuilder recordsBuilder(@NonNull ByteBufferOutputStream buffer, @NonNull MemoryRecords records) {
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

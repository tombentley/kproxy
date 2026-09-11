/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.record.manipulation.filter;

import java.nio.ByteBuffer;
import java.util.List;
import java.util.concurrent.CompletionStage;

import io.kroxylicious.filter.record.manipulation.kafka.RecordsDeserializer;
import io.kroxylicious.filter.record.manipulation.kafka.RecordsSerializer;
import io.kroxylicious.filter.record.manipulation.op.BaseTypedOp;
import io.kroxylicious.kafka.common.TopicPartition;
import io.kroxylicious.kafka.common.Uuid;
import io.kroxylicious.kafka.common.message.FetchResponseData;
import io.kroxylicious.kafka.common.message.ProduceRequestData;
import io.kroxylicious.kafka.common.message.RequestHeaderData;
import io.kroxylicious.kafka.common.message.ResponseHeaderData;
import io.kroxylicious.kafka.common.message.ShareFetchResponseData;
import io.kroxylicious.kafka.common.protocol.Errors;
import io.kroxylicious.kafka.common.record.internal.BaseRecords;
import io.kroxylicious.kafka.common.record.internal.MemoryRecords;
import io.kroxylicious.kafka.common.record.internal.Record;
import io.kroxylicious.kafka.transform.RecordStream;
import io.kroxylicious.proxy.filter.FetchResponseFilter;
import io.kroxylicious.proxy.filter.FilterContext;
import io.kroxylicious.proxy.filter.ProduceRequestFilter;
import io.kroxylicious.proxy.filter.RequestFilterResult;
import io.kroxylicious.proxy.filter.ResponseFilterResult;
import io.kroxylicious.proxy.filter.ShareFetchResponseFilter;
import io.kroxylicious.proxy.filter.metadata.TopicNameMapping;

import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.Nullable;

public class RecordManipulationFilter implements
        ProduceRequestFilter,
        FetchResponseFilter,
        ShareFetchResponseFilter {

    private final String topic;
    private final Direction direction;
    private final RecordsDeserializer deserializer;
    private final RecordsSerializer serializer;

    public RecordManipulationFilter(Init init) {
        this(init.topic(),
                init.direction(),
                init.recordTimestampPipeline(),
                init.recordKeyPipeline(),
                init.recordValuePipeline());
    }

    public RecordManipulationFilter(String topic,
                                    Direction direction,
                                    BaseTypedOp<Record, Long> recordTimestampPipeline,
                                    BaseTypedOp<Record, ByteBuffer> recordKeyPipeline,
                                    BaseTypedOp<Record, ByteBuffer> recordValuePipeline) {
        this.topic = topic;
        this.direction = direction;
        this.deserializer = new RecordsDeserializer();
        this.serializer = new RecordsSerializer(
                recordTimestampPipeline,
                recordKeyPipeline,
                recordValuePipeline);
    }

    @Override
    public CompletionStage<RequestFilterResult> onProduceRequest(short apiVersion,
                                                                 RequestHeaderData header,
                                                                 ProduceRequestData request,
                                                                 FilterContext context) {
        if (this.direction != Direction.IN) {
            return context.forwardRequest(header, request);
        }
        List<Uuid> idsToResolve = request.topicData().stream()
                .filter(topicData -> topicData.name().isEmpty())
                .map(ProduceRequestData.TopicProduceData::topicId)
                .toList();
        return context.topicNames(idsToResolve).thenCompose(topicNameMapping -> {
            for (var topicData : request.topicData()) {
                if (topic.equals(resolveTopicName(topicData.name(), topicData.topicId(), topicNameMapping))) {
                    for (var partitionData : topicData.partitionData()) {
                        partitionData.setRecords(transformRecords(partitionData.records(), partitionData.index()));
                    }
                }
            }
            return context.forwardRequest(header, request);
        });
    }

    @Override
    public CompletionStage<ResponseFilterResult> onFetchResponse(short apiVersion,
                                                                 ResponseHeaderData header,
                                                                 FetchResponseData response,
                                                                 FilterContext context) {
        if (this.direction != Direction.OUT) {
            return context.forwardResponse(header, response);
        }
        List<Uuid> idsToResolve = response.responses().stream()
                .filter(topicData -> topicData.topic().isEmpty())
                .map(FetchResponseData.FetchableTopicResponse::topicId)
                .toList();
        return context.topicNames(idsToResolve).thenCompose(topicNameMapping -> {
            for (var topicData : response.responses()) {
                if (topic.equals(resolveTopicName(topicData.topic(), topicData.topicId(), topicNameMapping))) {
                    for (var partitionData : topicData.partitions()) {
                        partitionData.setRecords(transformRecords(partitionData.records(), partitionData.partitionIndex()));
                    }
                }
            }
            return context.forwardResponse(header, response);
        });
    }

    /**
     * Resolves a topic's name, falling back to {@code topicNameMapping} when {@code name} is absent -
     * true from Produce (v13+, KIP-951) and Fetch (v13+, KIP-516) protocol versions onwards, which
     * identify topics by {@code topicId} alone.
     */
    @Nullable
    private static String resolveTopicName(String name, Uuid topicId, TopicNameMapping topicNameMapping) {
        return name.isEmpty() ? topicNameMapping.topicNames().get(topicId) : name;
    }

    @NonNull
    private MemoryRecords transformRecords(BaseRecords records,
                                           int partitionData) {
        RecordStream<TopicPartition> recordsWithTopicPartition = deserializer
                .apply(records)
                .mapConstant(new TopicPartition(topic, partitionData));
        return serializer.apply(recordsWithTopicPartition);
    }

    @Override
    public CompletionStage<ResponseFilterResult> onShareFetchResponse(short apiVersion,
                                                                      ResponseHeaderData header,
                                                                      ShareFetchResponseData response,
                                                                      FilterContext context) {
        if (this.direction == Direction.OUT) {
            return context.topicNames(response.responses().stream().map(ShareFetchResponseData.ShareFetchableTopicResponse::topicId).toList())
                    .thenCompose(topicMapping -> {
                        for (var topicData : response.responses()) {
                            if (topicMapping.failures().containsKey(topicData.topicId())) {
                                for (var partitionData : topicData.partitions()) {
                                    partitionData.setRecords(null);
                                    partitionData.setErrorCode(Errors.UNKNOWN_TOPIC_ID.code());
                                    partitionData.setErrorMessage("Oops");
                                }
                            }
                            else {
                                if (topicMapping.topicNames().get(topicData.topicId()).equals(topic)) {
                                    for (var partitionData : topicData.partitions()) {
                                        partitionData.setRecords(transformRecords(partitionData.records(), partitionData.partitionIndex()));
                                    }
                                }
                            }
                        }
                        return context.forwardResponse(header, response);
                    });
        }
        else {
            return context.forwardResponse(header, response);
        }

    }

}

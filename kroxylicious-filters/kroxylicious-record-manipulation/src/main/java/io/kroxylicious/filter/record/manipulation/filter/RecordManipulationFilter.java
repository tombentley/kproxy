/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.record.manipulation.filter;

import java.nio.ByteBuffer;
import java.util.concurrent.CompletionStage;

import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.message.FetchResponseData;
import org.apache.kafka.common.message.ProduceRequestData;
import org.apache.kafka.common.message.RequestHeaderData;
import org.apache.kafka.common.message.ResponseHeaderData;
import org.apache.kafka.common.message.ShareFetchResponseData;
import org.apache.kafka.common.protocol.Errors;
import org.apache.kafka.common.record.BaseRecords;
import org.apache.kafka.common.record.MemoryRecords;
import org.apache.kafka.common.record.Record;

import io.kroxylicious.filter.record.manipulation.kafka.RecordsDeserializer;
import io.kroxylicious.filter.record.manipulation.kafka.RecordsSerializer;
import io.kroxylicious.filter.record.manipulation.op.BaseTypedOp;
import io.kroxylicious.kafka.transform.RecordStream;
import io.kroxylicious.proxy.filter.FetchResponseFilter;
import io.kroxylicious.proxy.filter.FilterContext;
import io.kroxylicious.proxy.filter.ProduceRequestFilter;
import io.kroxylicious.proxy.filter.RequestFilterResult;
import io.kroxylicious.proxy.filter.ResponseFilterResult;
import io.kroxylicious.proxy.filter.ShareFetchResponseFilter;

import edu.umd.cs.findbugs.annotations.NonNull;

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
        if (this.direction == Direction.IN) {
            for (var topicData : request.topicData()) {
                if (topicData.name().equals(topic)) {
                    for (var partitionData : topicData.partitionData()) {
                        partitionData.setRecords(transformRecords(partitionData.records(), partitionData.index()));
                    }
                }
            }
        }
        return context.forwardRequest(header, request);
    }

    @Override
    public CompletionStage<ResponseFilterResult> onFetchResponse(short apiVersion,
                                                                 ResponseHeaderData header,
                                                                 FetchResponseData response,
                                                                 FilterContext context) {
        if (this.direction == Direction.OUT) {
            for (var topicData : response.responses()) {
                if (topicData.topic().equals(topic)) {
                    for (var partitionData : topicData.partitions()) {
                        partitionData.setRecords(transformRecords(partitionData.records(), partitionData.partitionIndex()));
                    }
                }
            }
        }
        return context.forwardResponse(header, response);
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

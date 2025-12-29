/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.transformation;

import java.util.Map;
import java.util.concurrent.CompletionStage;

import org.apache.kafka.common.message.ApiVersionsResponseData;
import org.apache.kafka.common.message.FetchResponseData;
import org.apache.kafka.common.message.ResponseHeaderData;
import org.apache.kafka.common.protocol.ApiKeys;
import org.apache.kafka.common.protocol.Errors;
import org.apache.kafka.common.record.BaseRecords;
import org.apache.kafka.common.record.MemoryRecords;
import org.apache.kafka.common.utils.ByteBufferOutputStream;

import io.kroxylicious.kafka.transform.RecordStream;
import io.kroxylicious.proxy.filter.ApiVersionsResponseFilter;
import io.kroxylicious.proxy.filter.FetchResponseFilter;
import io.kroxylicious.proxy.filter.FilterContext;
import io.kroxylicious.proxy.filter.ResponseFilterResult;

public class RecordTransformationFilter implements ApiVersionsResponseFilter, FetchResponseFilter {

    public static final short LATEST_FETCH_API_VERSION_USING_TOPIC_NAMES = (short) 12;
    Map<String, RecordTransform> transformations;

    public RecordTransformationFilter(Map<String, RecordTransform> transformations) {
        this.transformations = transformations;
    }

    @Override
    public CompletionStage<ResponseFilterResult> onFetchResponse(short apiVersion,
                                                                 ResponseHeaderData header,
                                                                 FetchResponseData response,
                                                                 FilterContext context) {
        for (var topicResponse : response.responses()) {
            String topicName = topicResponse.topic();
            var transformation = transformations.get(topicName);
            if (transformation != null) {
                for (var partitionResponse : topicResponse.partitions()) {
                    BaseRecords records = partitionResponse.records();
                    if (records != null) {
                        try {
                            var memoryRecords = ((MemoryRecords) records);
                            ByteBufferOutputStream byteBufferOutputStream = context.createByteBufferOutputStream(records.sizeInBytes());
                            var transformed = applyRecordTransformation(topicName, memoryRecords, byteBufferOutputStream, transformation);
                            partitionResponse.setRecords(transformed);
                        }
                        catch (Exception e) {
                            e.printStackTrace();
                            var error = Errors.forException(e);
                            partitionResponse.setErrorCode(error.code());
                            partitionResponse.setRecords(null);
                        }
                    }
                }
            }
        }
        return context.forwardResponse(header, response);
    }

    private static MemoryRecords applyRecordTransformation(String topicName,
                                                           MemoryRecords records,
                                                           ByteBufferOutputStream byteBufferOutputStream,
                                                           RecordTransform recordTransform) {
        return RecordStream.ofRecords(records)
                .toMemoryRecords(byteBufferOutputStream, new RecordTransformer(topicName, recordTransform));
    }

    @Override
    public CompletionStage<ResponseFilterResult> onApiVersionsResponse(short apiVersion,
                                                                       ResponseHeaderData header,
                                                                       ApiVersionsResponseData response,
                                                                       FilterContext context) {
        response.apiKeys().find(ApiKeys.FETCH.id).setMaxVersion(LATEST_FETCH_API_VERSION_USING_TOPIC_NAMES);
        return context.forwardResponse(header, response);
    }
}

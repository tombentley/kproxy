/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.transformation;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.util.Map;
import java.util.concurrent.CompletionStage;

import org.apache.kafka.common.header.Header;
import org.apache.kafka.common.message.FetchResponseData;
import org.apache.kafka.common.message.ResponseHeaderData;
import org.apache.kafka.common.protocol.Errors;
import org.apache.kafka.common.record.BaseRecords;
import org.apache.kafka.common.record.MemoryRecords;
import org.apache.kafka.common.record.Record;
import org.apache.kafka.common.record.RecordBatch;
import org.apache.kafka.common.utils.ByteBufferOutputStream;

import io.kroxylicious.kafka.transform.RecordStream;
import io.kroxylicious.kafka.transform.RecordTransform;
import io.kroxylicious.proxy.filter.FetchResponseFilter;
import io.kroxylicious.proxy.filter.FilterContext;
import io.kroxylicious.proxy.filter.ResponseFilterResult;

import edu.umd.cs.findbugs.annotations.Nullable;

public class RecordTransformationFilter implements FetchResponseFilter {

    Map<String, RecordTransformation> transformations;

    @Override
    public CompletionStage<ResponseFilterResult> onFetchResponse(short apiVersion,
                                                                 ResponseHeaderData header,
                                                                 FetchResponseData response,
                                                                 FilterContext context) {
        for (var topicResponse : response.responses()) {
            var transformation = transformations.get(topicResponse.topic());
            if (transformation != null) {
                for (var partitionResponse : topicResponse.partitions()) {
                    BaseRecords records = partitionResponse.records();
                    if (records != null) {
                        try {
                            var memoryRecords = ((MemoryRecords) records);
                            ByteBufferOutputStream byteBufferOutputStream = context.createByteBufferOutputStream(records.sizeInBytes());
                            var transformed = applyRecordTransformation(memoryRecords, byteBufferOutputStream, transformation);
                            partitionResponse.setRecords(transformed);
                        }
                        catch (Exception e) {
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


    private static MemoryRecords applyRecordTransformation(MemoryRecords records,
                                                           ByteBufferOutputStream byteBufferOutputStream,
                                                           RecordTransformation recordTransformation) {
        return RecordStream.ofRecords(records)
                .toMemoryRecords(byteBufferOutputStream, new VoidRecordTransform(recordTransformation));
    }

    private static ByteBuffer applyBufferTransformation(int initialCapacity,
                                                        Header[] headers,
                                                        ByteBuffer srcBuffer,
                                                        DatumTransformation datumTransformation) {
        TransformationInputStream in = new TransformationInputStream(srcBuffer);
        TransformationOutputStream out = new TransformationOutputStream(initialCapacity);
        try {
            datumTransformation.apply(headers, in, out);
            return out.toByteBuffer();
        }
        catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @SuppressWarnings("java:S6213") // `record` is a perfectly acceptable identifier
    private static class VoidRecordTransform implements RecordTransform<Void> {

        private final RecordTransformation recordTransformation;
        private Header[] transformedHeaders;
        private ByteBuffer transformedKey;
        private ByteBuffer transformedValue;

        VoidRecordTransform(RecordTransformation recordTransformation) {
            this.recordTransformation = recordTransformation;
        }

        @Override
        public void initBatch(RecordBatch batch) {
            // Nothing needs to be done
        }

        @Override
        public void init(@Nullable Void state, Record record) {
            this.transformedHeaders = recordTransformation.headerTransformation().transformHeaders(record.headers());
            this.transformedKey = applyBufferTransformation(2 * record.keySize(),
                    record.headers(),
                    record.key(),
                    recordTransformation.keyTransformation());
            this.transformedValue = applyBufferTransformation(2 * record.valueSize(),
                    record.headers(),
                    record.value(),
                    recordTransformation.valueTransformation());
        }

        @Override
        public void resetAfterTransform(Void state, Record record) {
            this.transformedHeaders = null;
            this.transformedKey = null;
            this.transformedValue = null;
        }

        @Override
        public long transformOffset(Record record) {
            return record.offset();
        }

        @Override
        public long transformTimestamp(Record record) {
            return record.timestamp();
        }

        @Override
        public ByteBuffer transformKey(Record record) {
            return transformedKey;
        }

        @Override
        public ByteBuffer transformValue(Record record) {
            return transformedValue;
        }

        @Override
        public Header[] transformHeaders(Record record) {
            return transformedHeaders;
        }
    }
}

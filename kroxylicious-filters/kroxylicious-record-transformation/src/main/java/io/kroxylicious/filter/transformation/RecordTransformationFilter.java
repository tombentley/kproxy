/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.transformation;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
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



    @SuppressWarnings("java:S6213") // `record` is a perfectly acceptable identifier
    private static class VoidRecordTransform implements RecordTransform<Void> {

        private final RecordTransformation recordTransformation;
        private Header[] transformedHeaders;
        private ByteBuffer transformedKey;
        private ByteBuffer transformedValue;
        private static final VarHandle transformedKeyHandle;
        private static final VarHandle transformedValueHandle;
        static {
            try {
                transformedKeyHandle = MethodHandles.lookup().findVarHandle(VoidRecordTransform.class, "transformedKey", ByteBuffer.class);
                transformedValueHandle = MethodHandles.lookup().findVarHandle(VoidRecordTransform.class, "transformedValue", ByteBuffer.class);
            }
            catch (NoSuchFieldException | IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        }

        VoidRecordTransform(RecordTransformation recordTransformation) {
            this.recordTransformation = recordTransformation;
        }

        @Override
        public void initBatch(RecordBatch batch) {
            // Nothing needs to be done
        }

        @Override
        public void init(@Nullable Void state, Record record) {
            this.transformedKey = applyBufferTransformation(2 * record.keySize(),
                    record.headers(),
                    record.key(),
                    recordTransformation.keyTransformation());
            this.transformedValue = applyBufferTransformation(2 * record.valueSize(),
                    record.headers(),
                    record.value(),
                    recordTransformation.valueTransformation());
            this.transformedHeaders = recordTransformation.headerTransformation().transformHeaders(record.headers());
        }

        private static ByteBuffer applyBufferTransformation(int initialCapacity,
                                                            Header[] headers,
                                                            ByteBuffer srcBuffer,
                                                            DatumTransformation datumTransformation) {
            TransformationInputStream in = new TransformationInputStream(srcBuffer);
            TransformationOutputStream out = new TransformationOutputStream(initialCapacity);
            try {
                var datum = datumTransformation.deserializer().deserialize(headers, in);
                var originalSchema = datum.schemaIdentifier();
                var value = datum.datum();
                var type = datum.type();
                for (DatumMapper mapper : datumTransformation.mappers()) {
                    value = mapper.transform(value);
                    type = mapper.returnedType();
                    if (!type.isInstance(value)) {
                        throw new RuntimeException();
                    }
                }
                // Idea is to split the serialization, so that there's a dedicated serializer for schema ids
                // and a dedicated serializer for values
                // Also split up the transformation of schemaIdentifiers as a preprocessing step
                // So:
                // 1. Transform the key -> Datum
                // 1. Transform the value -> Datum
                // Final transform on just the schema ids
                // 1. Serialize the key schema id -> Headers?
                // 3. Serialize the key -> prefix?
                // 2. Serialize the value schema id -> Headers?
                // 4. Serialize the value -> prefix?
                // 5. Transform the headers
                // 6. Combine the headers
                //datum.schemaIdentifier()
                var finalDatum = new Datum(null, type, value);
                ((DatumSerializer) datumTransformation.serializer()).serialize(finalDatum, out);
                return out.toByteBuffer();
            }
            catch (IOException e) {
                throw new UncheckedIOException(e);
            }
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

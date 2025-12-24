/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.transformation;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.kafka.common.header.Header;
import org.apache.kafka.common.record.CompressionType;
import org.apache.kafka.common.record.Record;
import org.apache.kafka.common.record.RecordBatch;

import io.kroxylicious.filter.transformation.api.format.Deserializer;
import io.kroxylicious.filter.transformation.api.format.Serializer;
import io.kroxylicious.filter.transformation.api.mapper.Mapper;

import edu.umd.cs.findbugs.annotations.Nullable;

/**
 * The thing that actually transforms records.
 */
@SuppressWarnings("java:S6213") // `record` is a perfectly acceptable identifier
class RecordTransform implements io.kroxylicious.kafka.transform.RecordTransform<Void> {

    private final String topicName;
    private final RecordTransformation recordTransformation;
    private Header[] transformedHeaders;
    private ByteBuffer transformedKey;
    private ByteBuffer transformedValue;
    private TransformationOutputStream keyOut;
    private TransformationOutputStream valueOut;

    RecordTransform(String topicName, RecordTransformation recordTransformation) {
        this.topicName = topicName;
        this.recordTransformation = recordTransformation;
    }

    @Override
    public void initBatch(RecordBatch batch) {
        // Try to use a
        int initialCapacity = Math.min(estimatedRecordSize(batch), 50_000);
        keyOut = new TransformationOutputStream(initialCapacity);
        valueOut = new TransformationOutputStream(initialCapacity);
    }

    private static int estimatedRecordSize(RecordBatch batch) {
        int initialCapacity;
        Integer numRecords = batch.countOrNull();
        if (numRecords != null) {
            // start with double the mean record size
            initialCapacity = 2 * (batch.sizeInBytes() / numRecords);
            if (batch.compressionType() != CompressionType.NONE) {
                // 5:1 is a typical "good" compression ratio for formats which compress well
                initialCapacity *= 5;
            }
            initialCapacity = Math.min(initialCapacity, batch.sizeInBytes());
        }
        else {
            initialCapacity = batch.sizeInBytes();
        }
        return initialCapacity;
    }

    @Override
    public void init(@Nullable Void state, org.apache.kafka.common.record.Record record) {
        try {

            var keySchemaHeaders = applyBufferTransformation(
                    RecordDataLocation.KeyDataLocation.INSTANCE,
                    record,
                    keyOut
            );
            this.transformedKey = keyOut.toByteBuffer();

            var valueSchemaHeaders = applyBufferTransformation(
                    RecordDataLocation.ValueDataLocation.INSTANCE,
                    record,
                    valueOut
            );
            this.transformedValue = valueOut.toByteBuffer();

            this.transformedHeaders = transformedHeaders(record, keySchemaHeaders, valueSchemaHeaders);
        }
        catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private Header[] transformedHeaders(org.apache.kafka.common.record.Record record,
                                        List<Header> keySchemaHeaders,
                                        List<Header> valueSchemaHeaders) {
        // TODO detect conflicts.
        // TODO decide whether we remove schema headers before the headTransformation or afterwards
        //   if before then we can detect and give a good error if the user has added those headers
        var keysToRemove = Stream.concat(keySchemaHeaders.stream(), valueSchemaHeaders.stream())
                .map(Header::key)
                .collect(Collectors.toSet());
        var headers = removeHeadersWithKeys(
                recordTransformation.headerTransformation().transformHeaders(List.of(record.headers())),
                keysToRemove);
        headers.addAll(keySchemaHeaders);
        headers.addAll(valueSchemaHeaders);
        return headers.toArray(new Header[0]);
    }

    ArrayList<Header> removeHeadersWithKeys(List<Header> headers, Set<String> keys) {
        return headers.stream().filter(header -> !keys.contains(header.key())).collect(Collectors.toCollection(ArrayList::new));
    }

    private List<Header> applyBufferTransformation(RecordDataLocation dataLocation,
                                                   Record record,
                                                   TransformationOutputStream out) throws IOException {

        ByteBuffer buffer = dataLocation.buffer(record);
        // TODO think about how we handle null buffers. null buffer != empty buffer != null value (e.g. json null) != empty value (e.g. json empty array).
        //  There's nothing to deserialize so skip the deserialiser but pass null into the mapper chain?
        //  Or skip the chain entirely?
        //  If the chain can return null then we also need to handle that in serializers
        //  Is this a reason to have a Datum wrapper so that null != Datum(null)
        TransformationInputStream in = new TransformationInputStream(buffer != null ? buffer : ByteBuffer.wrap(new byte[0]));

        // First obtain the schema id
        var originalSchemaId = dataLocation.inputSchemaIdentification(recordTransformation).schemaIdFromData(List.of(record.headers()), dataLocation, in);
        var finalSchemaId = dataLocation.schemaTransformation(recordTransformation).schemaIdentifier(topicName, dataLocation, originalSchemaId);
        var schemaIdentificationStrategy = dataLocation.outputSchemaIdentification(recordTransformation);
        var headers = schemaIdentificationStrategy.headers(finalSchemaId, dataLocation);

        // Then execute the pipeline
        Deserializer<?> deserializer = dataLocation.deserializer(recordTransformation);
        var value = deserializer.deserialize(in);
        var type = deserializer.returnedType();
        if (!type.isInstance(value)) {
            throw new RuntimeException();
        }
        for (Mapper mapper : dataLocation.mappers(recordTransformation)) {
            value = mapper.transform(value);
            type = mapper.returnedType();
            if (!type.isInstance(value)) {
                throw new RuntimeException();
            }
        }

        out.write(schemaIdentificationStrategy.prefix(finalSchemaId));
        ((Serializer) dataLocation.serializer(recordTransformation)).serialize(value, out);

        return headers;
    }

    @Override
    public void resetAfterTransform(Void state, org.apache.kafka.common.record.Record record) {
        this.transformedHeaders = null;
        this.transformedKey = null;
        this.transformedValue = null;
        keyOut.reset();
        valueOut.reset();
    }

    @Override
    public long transformOffset(org.apache.kafka.common.record.Record record) {
        return record.offset();
    }

    @Override
    public long transformTimestamp(org.apache.kafka.common.record.Record record) {
        return record.timestamp();
    }

    @Override
    public ByteBuffer transformKey(org.apache.kafka.common.record.Record record) {
        return transformedKey;
    }

    @Override
    public ByteBuffer transformValue(org.apache.kafka.common.record.Record record) {
        return transformedValue;
    }

    @Override
    public Header[] transformHeaders(Record record) {
        return transformedHeaders;
    }
}

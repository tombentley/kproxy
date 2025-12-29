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
import io.kroxylicious.filter.transformation.api.mapper.Context;
import io.kroxylicious.filter.transformation.api.mapper.Mappers;
import io.kroxylicious.filter.transformation.api.schema.identification.WireSchemaId;
import io.kroxylicious.filter.transformation.api.schema.registry.SchemaRegistry;

import edu.umd.cs.findbugs.annotations.Nullable;

/**
 * The thing that actually transforms records.
 */
@SuppressWarnings("java:S6213") // `record` is a perfectly acceptable identifier
class RecordTransformer implements io.kroxylicious.kafka.transform.RecordTransform<Void> {

    private final String topicName;
    private final RecordTransform recordTransform;
    private Header[] transformedHeaders;
    private ByteBuffer transformedKey;
    private ByteBuffer transformedValue;
    private TransformationOutputStream keyOut;
    private TransformationOutputStream valueOut;

    RecordTransformer(String topicName, RecordTransform recordTransform) {
        this.topicName = topicName;
        this.recordTransform = recordTransform;
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
                    RecordDataLocation.KEY,
                    record,
                    keyOut
            );
            this.transformedKey = keyOut.toByteBuffer();

            var valueSchemaHeaders = applyBufferTransformation(
                    RecordDataLocation.VALUE,
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
        // TODO detect header key conflicts.
        // TODO decide whether we remove schema headers before the headTransformation or afterwards
        //   if before then we can detect and give a good error if the user has added those headers
        var keysToRemove = Stream.concat(keySchemaHeaders.stream(), valueSchemaHeaders.stream())
                .map(Header::key)
                .collect(Collectors.toSet());
        var context = new Context(this.topicName, List.of(record.headers()), null); // TODO there is no location here
        var headers = removeHeadersWithKeys(
                this.recordTransform.headerTransformation().transform(List.of(record.headers()), context),
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

        Context context = new Context(topicName, List.of(record.headers()), dataLocation);

        // First obtain the schema id
        WireSchemaId originalSchemaId = dataLocation.schemaIdTransform(recordTransform).inputSchemaIdentification()
                .deserialize(in, context);
        //extracted(originalSchemaId);

        // Transform the schema id

        var finalSchemaId = dataLocation.schemaIdTransform(recordTransform).schemaIdTransformation()
                .transform(originalSchemaId, context);
        var schemaIdentificationStrategy = dataLocation.schemaIdTransform(recordTransform).outputschemaIdentification();
        var schemaHeaders = schemaIdentificationStrategy.headers(finalSchemaId, dataLocation);

        // Then execute the pipeline
        Deserializer<?> deserializer = dataLocation.dataTransform(recordTransform).deserializer();
        var value = deserializer.deserialize(in, new Context("test-topic", List.of(), RecordDataLocation.KEY));
        var type = deserializer.returnedType();
        if (!type.isInstance(value)) {
            throw new TypeException("value was of type " + value.getClass().getName() + " which is not an instance of type " + type.getName());
        }
        DataTransform dataTransform = dataLocation.dataTransform(recordTransform);
        if (dataTransform.mapperOpt().isPresent()) {
            Mapper mapper = dataTransform.mapperOpt().orElse(Mappers.identity(dataTransform.deserializer().returnedType()));
            value = mapper.transform(value, context);
            type = mapper.returnedType();
            if (!type.isInstance(value)) {
                throw new TypeException("value was of type " + value.getClass().getName() + " which is not an instance of type " + type.getName());
            }
        }

        out.write(schemaIdentificationStrategy.prefix(finalSchemaId));
        ((Serializer) dataLocation.dataTransform(recordTransform).serializer()).serialize(value, out);

        return schemaHeaders;
    }

    private static Deserializer<?> extracted(WireSchemaId originalSchemaId) {
        // TODO now, maybe we need to fetch the schema from a registry before we can deserialize it
        //   (but only if we actually need to deserialize the data; i.e. this is not a schema id only transformation)
        SchemaRegistry registry = null;
        var df = registry.getSchema(originalSchemaId).toCompletableFuture().join();
        return df.deserializer();
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

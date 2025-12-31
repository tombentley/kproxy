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
import java.util.function.BiFunction;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.kafka.common.header.Header;
import org.apache.kafka.common.record.CompressionType;
import org.apache.kafka.common.record.Record;
import org.apache.kafka.common.record.RecordBatch;

import io.kroxylicious.filter.transformation.api.RecordDataLocation;
import io.kroxylicious.filter.transformation.api.SchemaAndValue;
import io.kroxylicious.filter.transformation.api.Type;
import io.kroxylicious.filter.transformation.api.format.DataFormat;
import io.kroxylicious.filter.transformation.api.format.Deserializer;
import io.kroxylicious.filter.transformation.api.format.Serializer;
import io.kroxylicious.filter.transformation.api.mapper.Context;
import io.kroxylicious.filter.transformation.api.mapper.DataMapping;
import io.kroxylicious.filter.transformation.api.schema.identification.NoSchemaId;
import io.kroxylicious.filter.transformation.api.schema.identification.SchemaIdSerializer;
import io.kroxylicious.filter.transformation.api.schema.identification.WireSchemaId;
import io.kroxylicious.filter.transformation.model.RecordTransform;

import edu.umd.cs.findbugs.annotations.Nullable;

/**
 * The thing that actually transforms records.
 */
@SuppressWarnings("java:S6213") // `record` is a perfectly acceptable identifier
class SchemalessRecordTransformer implements io.kroxylicious.kafka.transform.RecordTransform<Void> {

    private final boolean typeCheck = false;
    private final String topicName;
    private final RecordTransform recordTransform;
    private final BiFunction<Context, Record, DataFormat<?, ?>> dataFormatFunction;

    private Header[] transformedHeaders;
    private ByteBuffer transformedKey;
    private ByteBuffer transformedValue;
    private TransformationOutputStream keyOut;
    private TransformationOutputStream valueOut;

    SchemalessRecordTransformer(String topicName,
                                RecordTransform recordTransform,
                                BiFunction<Context, Record, DataFormat<?, ?>> dataFormatFunction) {
        this.topicName = topicName;
        this.recordTransform = recordTransform;
        this.dataFormatFunction = dataFormatFunction;
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
    public void init(@Nullable Void state, Record record) {
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

    private Header[] transformedHeaders(Record record,
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
        Context context = new Context(topicName, List.of(record.headers()), dataLocation);
        var dataFormat = dataFormatFunction.apply(context, record);
        var deserializer = dataFormat.deserializer(dataFormat.defaultEncoding());
        var serializer = dataFormat.serializer(dataFormat.defaultEncoding());
        DataMapping dataMapping = dataLocation.dataTransform(recordTransform).mapperOpt().get();
        SchemaIdSerializer schemaIdSerializer = dataLocation.dataTransform(recordTransform).schemaIdSerializer();
        if (typeCheck) {
            Type<?, ?, ?> type = deserializer.typeCheck(Type.fromBytes());
            type = dataMapping.typeCheck(type);
            type = schemaIdSerializer.typeCheck(type);
            serializer.accepts(type);
        }
        return transform(context, record, deserializer, dataMapping, schemaIdSerializer, serializer, out);
    }

    private <W extends WireSchemaId, S, V,
            W2 extends WireSchemaId, S2, V2> List<Header> transform(Context context,
                                                                    Record record,
                                                                    Deserializer<S, V> deserializer,
                                                                    DataMapping<W, S, V, W2, S2, V2> dataMapping,
                                                                    SchemaIdSerializer<W2> schemaIdSerializer,
                                                                    Serializer<V2> serializer,
                                                                    TransformationOutputStream out) throws IOException {
        ByteBuffer buffer = context.location().buffer(record);
        // TODO think about how we handle null buffers. null buffer != empty buffer != null value (e.g. json null) != empty value (e.g. json empty array).
        //  There's nothing to deserialize so skip the deserialiser but pass null into the mapper chain?
        //  Or skip the chain entirely?
        //  If the chain can return null then we also need to handle that in serializers
        //  Is this a reason to have a Datum wrapper so that null != Datum(null)
        TransformationInputStream in = new TransformationInputStream(buffer != null ? buffer : ByteBuffer.wrap(new byte[0]));
        SchemaAndValue<NoSchemaId, S, V> deserialized = deserializer.deserialize(in, context);

        SchemaAndValue<W, S, V> deserialized2 = new SchemaAndValue<>((W) deserialized.schemaId(), deserialized.schema(), deserialized.value());

        SchemaAndValue<W2, S2, V2> mapped = dataMapping.transform(deserialized2, context);

        var schemaHeaders = schemaIdSerializer.serializeSchemaId(context.location(), mapped.schemaId(), out);
        serializer.serialize(mapped.value(), out);
        return schemaHeaders;
    }

    @Override
    public void resetAfterTransform(Void state, Record record) {
        this.transformedHeaders = null;
        this.transformedKey = null;
        this.transformedValue = null;
        keyOut.reset();
        valueOut.reset();
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

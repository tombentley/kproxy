/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.transformation;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.message.ApiVersionsResponseData;
import org.apache.kafka.common.message.FetchResponseData;
import org.apache.kafka.common.message.ResponseHeaderData;
import org.apache.kafka.common.protocol.ApiKeys;
import org.apache.kafka.common.protocol.Errors;
import org.apache.kafka.common.record.BaseRecords;
import org.apache.kafka.common.record.MemoryRecords;
import org.apache.kafka.common.record.Record;
import org.apache.kafka.common.utils.ByteBufferOutputStream;

import io.kroxylicious.filter.transformation.api.RecordDataLocation;
import io.kroxylicious.filter.transformation.api.format.DataFormat;
import io.kroxylicious.filter.transformation.api.mapper.Context;
import io.kroxylicious.filter.transformation.api.schema.identification.SchemaIdDeserializer;
import io.kroxylicious.filter.transformation.api.schema.identification.WireSchemaId;
import io.kroxylicious.filter.transformation.api.schema.registry.ResolvedSchema;
import io.kroxylicious.filter.transformation.api.schema.registry.SchemaRegistry;
import io.kroxylicious.filter.transformation.api.schema.registry.UnsupportedSchemaIdTypeException;
import io.kroxylicious.filter.transformation.format.avro.AvroFormat;
import io.kroxylicious.filter.transformation.format.avro.AvroSchemaDeserializer;
import io.kroxylicious.filter.transformation.format.json.JsonFormat;
import io.kroxylicious.filter.transformation.model.LateBoundDataTransform;
import io.kroxylicious.filter.transformation.model.RecordTransform;
import io.kroxylicious.filter.transformation.model.EarlyBoundDataTransform;
import io.kroxylicious.kafka.transform.RecordStream;
import io.kroxylicious.proxy.filter.ApiVersionsResponseFilter;
import io.kroxylicious.proxy.filter.FetchResponseFilter;
import io.kroxylicious.proxy.filter.FilterContext;
import io.kroxylicious.proxy.filter.ResponseFilterResult;

import edu.umd.cs.findbugs.annotations.NonNull;

@SuppressWarnings("java:S6213") // `record` is a perfectly acceptable identifier
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
        Map<TopicPartition, CompletableFuture<MemoryRecords>> incomplete = new HashMap<>();
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
                            if (transformed.isDone()) {
                                partitionResponse.setRecords(transformed.join());
                            }
                            else {
                                incomplete.put(new TopicPartition(topicName, partitionResponse.partitionIndex()), transformed);
                            }
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
        if (incomplete.isEmpty()) {
            return context.forwardResponse(header, response);
        }
        else {
            return CompletableFuture.allOf(incomplete.values().toArray(CompletableFuture[]::new)).thenCompose(
            ignored -> {
                for (var topicResponse : response.responses()) {
                    String topicName = topicResponse.topic();
                    for (var partitionResponse : topicResponse.partitions()) {
                        BaseRecords records = partitionResponse.records();
                        if (records != null) {
                            var completedFuture = incomplete.get(
                                    new TopicPartition(topicName, partitionResponse.partitionIndex()));
                            if (completedFuture != null) {
                                try {
                                    partitionResponse.setRecords(completedFuture.join());
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
            });
        }
    }

    private static CompletableFuture<MemoryRecords> applyRecordTransformation(String topicName,
                                                           MemoryRecords records,
                                                           ByteBufferOutputStream byteBufferOutputStream,
                                                           RecordTransform recordTransform) {

        Set<WireSchemaId> schemaIds = schemaIds(topicName, records, recordTransform);

        if (schemaIds.isEmpty()) {
            BiFunction<Context, Record, DataFormat<?, ?>> dataFormatFunction = (context, record) -> {
                if (context.location().dataTransform(recordTransform) instanceof EarlyBoundDataTransform<?, ?, ?, ?, ?, ?> dataTransform) {
                    return dataTransform.dataFormat();
                }
                else {
                    throw new RecordTransformationException("WTF");
                }
            };

            // iteration!
            return CompletableFuture.completedFuture(transformMemoryRecords(topicName, records, byteBufferOutputStream, recordTransform, dataFormatFunction));
        }
        else {
            SchemaRegistry registry = null;
            var notSupported = schemaIds.stream().map(WireSchemaId::getClass).filter(schemaId -> !registry.supports(schemaId)).map(Class::getName).collect(Collectors.joining(", "));
            if (!notSupported.isEmpty()) {
                throw new UnsupportedSchemaIdTypeException(
                        String.format("Schema registry %s does not support schema ids of type %s",
                        registry, notSupported));
            }
            // At least one datum in at least one of the records depends on a late-bound schema
            CompletableFuture[] futures = schemaIds.stream().map(registry::getSchema).toArray(CompletableFuture[]::new);
            return CompletableFuture.allOf(futures).thenApply(
                    ignored -> {
                        var formatMap = Arrays.stream(futures)
                                .map(future -> ((ResolvedSchema) future.join()))
                                .collect(Collectors.toMap(ResolvedSchema::schemaId, RecordTransformationFilter::readSchema));

                        BiFunction<Context, Record, DataFormat<?, ?>> formatFunction = (context, record) -> {
                            try {
                                var schemaIdDeserializer = recordTransform.keyTransform().schemaIdDeserializer();
                                WireSchemaId wireSchemaId = SchemaIdConsumer.schemaId(record, context, schemaIdDeserializer);
                                return formatMap.get(wireSchemaId);
                            }
                            catch (Exception e) {
                                throw new RecordTransformationException(e);
                            }
                        };

                        return transformMemoryRecords(topicName, records, byteBufferOutputStream, recordTransform, formatFunction);
                    });
        }
    }

    @NonNull
    private static MemoryRecords transformMemoryRecords(String topicName,
                                                        MemoryRecords records,
                                                        ByteBufferOutputStream byteBufferOutputStream,
                                                        RecordTransform recordTransform,
                                                        BiFunction<Context, Record, DataFormat<?, ?>> formatFunction) {
        return RecordStream.ofRecords(records)
                .toMemoryRecords(byteBufferOutputStream,
                        new SchemalessRecordTransformer(topicName, recordTransform, formatFunction));
    }

    @NonNull
    private static Set<WireSchemaId> schemaIds(String topicName,
                                               MemoryRecords records,
                                               RecordTransform recordTransform) {
        var lateBound = new HashMap<RecordDataLocation, SchemaIdDeserializer<?>>(5);
        if (recordTransform.keyTransform() instanceof LateBoundDataTransform<?, ?, ?, ?, ?, ?> keyTransform) {
            lateBound.put(RecordDataLocation.KEY, keyTransform.schemaIdDeserializer());
        }
        if (recordTransform.valueTransform() instanceof LateBoundDataTransform<?, ?, ?, ?, ?, ?> valueTransform) {
            lateBound.put(RecordDataLocation.VALUE, valueTransform.schemaIdDeserializer());
        }
        Set<WireSchemaId> schemaIds;
        if (!lateBound.isEmpty()) {
            // iteration!
            schemaIds = SchemaIdConsumer.schemaIds(topicName, lateBound, records);
        }
        else {
            schemaIds = Collections.emptySet();
        }
        return schemaIds;
    }

    private static DataFormat<?, ?> readSchema(ResolvedSchema resolvedSchema) {
        return switch (resolvedSchema.schemaType()) {
            case "jsonschema" -> JsonFormat.INSTANCE; // this is a bit of a lie
            case "avro" -> {
                try {
                    yield new AvroFormat(resolvedSchema.schemaId(), new AvroSchemaDeserializer()
                            .deserialize(new ByteArrayInputStream(resolvedSchema.schema())));
                }
                catch (IOException e) {
                    throw new RecordTransformationException("Unabled to deserialize Avro schema", e);
                }
            }
            default -> throw new IllegalStateException("Unsupported schema type: " + resolvedSchema.schemaType());
        };
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

/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.transformation;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import org.apache.kafka.common.compress.Compression;
import org.apache.kafka.common.header.Header;
import org.apache.kafka.common.message.ApiVersionsResponseData;
import org.apache.kafka.common.message.FetchResponseData;
import org.apache.kafka.common.message.ResponseHeaderData;
import org.apache.kafka.common.protocol.ApiKeys;
import org.apache.kafka.common.protocol.ApiMessage;
import org.apache.kafka.common.protocol.Errors;
import org.apache.kafka.common.record.MemoryRecords;
import org.apache.kafka.common.record.RecordBatch;
import org.apache.kafka.common.record.SimpleRecord;
import org.apache.kafka.common.record.TimestampType;
import org.apache.kafka.common.utils.ByteBufferOutputStream;
import org.assertj.core.api.Assertions;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import io.kroxylicious.filter.transformation.api.mapper.Mapper;
import io.kroxylicious.filter.transformation.api.mapper.Mappers;
import io.kroxylicious.filter.transformation.api.schema.identification.NoSchema;
import io.kroxylicious.filter.transformation.api.schema.identification.NoSchemaIdDeserializer;
import io.kroxylicious.filter.transformation.api.schema.identification.NoSchemaSerializer;
import io.kroxylicious.filter.transformation.format.bytes.BytesDeserializer;
import io.kroxylicious.filter.transformation.format.bytes.BytesSerializer;
import io.kroxylicious.filter.transformation.format.json.JsonDeserializer;
import io.kroxylicious.filter.transformation.format.json.JsonSerializer;
import io.kroxylicious.filter.transformation.mapper.json.ApplyJsonPatchTest;
import io.kroxylicious.kafka.transform.BatchAwareMemoryRecordsBuilder;
import io.kroxylicious.proxy.filter.FilterContext;
import io.kroxylicious.test.assertj.KafkaAssertions;

import edu.umd.cs.findbugs.annotations.NonNull;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RecordTransformFilterTest {

    @Mock
    FilterContext filterContext;

    @Captor
    ArgumentCaptor<ApiMessage> argCaptor;


    @NonNull
    private static RecordTransform jsonTransformation(
            List<Mapper<?, ?>> keyMappers,
            List<Mapper<?, ?>> valueMappers) {
        return new RecordTransform(
                Mappers.identityHeaders(),

                new SchemaIdTransform(NoSchemaIdDeserializer.INSTANCE,
                        Mappers.preserve(NoSchema.class),
                        NoSchemaSerializer.INSTANCE),
                new DataTransform(JsonDeserializer.INSTANCE,
                        keyMappers,
                        JsonSerializer.INSTANCE),

                new SchemaIdTransform(NoSchemaIdDeserializer.INSTANCE,
                        Mappers.preserve(NoSchema.class),
                        NoSchemaSerializer.INSTANCE),
                new DataTransform(JsonDeserializer.INSTANCE,
                        valueMappers,
                        JsonSerializer.INSTANCE));
    }

    @NonNull
    private static RecordTransform bytesTransformation(
            List<Mapper<?, ?>> keyMappers,
            List<Mapper<?, ?>> valueMappers) {
        return new RecordTransform(
                Mappers.identityHeaders(),
                new SchemaIdTransform(NoSchemaIdDeserializer.INSTANCE,
                        Mappers.preserve(NoSchema.class),
                        NoSchemaSerializer.INSTANCE),
                new DataTransform(BytesDeserializer.INSTANCE,
                        keyMappers,
                        BytesSerializer.INSTANCE),
                new SchemaIdTransform(NoSchemaIdDeserializer.INSTANCE,
                        Mappers.preserve(NoSchema.class),
                        NoSchemaSerializer.INSTANCE),
                new DataTransform(BytesDeserializer.INSTANCE,
                        valueMappers,
                        BytesSerializer.INSTANCE));
    }

    @NonNull
    private static MemoryRecords singletonBatch(byte[] key, byte[] value) {
        var builder = new BatchAwareMemoryRecordsBuilder(new ByteBufferOutputStream(100));
        builder.addBatch(RecordBatch.CURRENT_MAGIC_VALUE,
                Compression.NONE,
                TimestampType.CREATE_TIME,
                0,
                0,
                0,
                (short) 0,
                0,
                false,
                false,
                0,
                0);
        builder.append(new SimpleRecord(0,
                key,
                value,
                new Header[0]));
        var records = builder.build();
        return records;
    }


    @NonNull
    private static MemoryRecords assertSingletonPartitionNoErrors(List<FetchResponseData.FetchableTopicResponse> responses) {
        var topicAssert = Assertions.assertThat(responses)
                .hasSize(1)
                .singleElement();
        topicAssert.extracting(FetchResponseData.FetchableTopicResponse::topic).isEqualTo("my-topic");
        var partitionAssert = topicAssert.extracting(FetchResponseData.FetchableTopicResponse::partitions)
                .asInstanceOf(InstanceOfAssertFactories.list(FetchResponseData.PartitionData.class))
                .hasSize(1)
                .singleElement();
        partitionAssert.extracting(FetchResponseData.PartitionData::partitionIndex).isEqualTo(78);
        partitionAssert.extracting(FetchResponseData.PartitionData::errorCode)
                .as("Expect partition errorCode==NONE")
                .isEqualTo(Errors.NONE.code());
        return (MemoryRecords) partitionAssert.actual().records();
    }

    @Test
    void shouldReturnApiVersionsAvoidingTopicIds() {

        var filter = new RecordTransformationFilter(Map.of("my-topic", jsonTransformation(
                List.of(),
                List.of())
        ));

        short firstVersionUsingTopicIds = (short) (RecordTransformationFilter.LATEST_FETCH_API_VERSION_USING_TOPIC_NAMES + 1);

        ApiVersionsResponseData.ApiVersionCollection v = new ApiVersionsResponseData.ApiVersionCollection();
        v.mustAdd(new ApiVersionsResponseData.ApiVersion()
                        .setApiKey(ApiKeys.FETCH.id)
                        .setMaxVersion(firstVersionUsingTopicIds)
                        .setMinVersion(ApiKeys.FETCH.oldestVersion()));

        // When
        filter.onApiVersionsResponse(
                ApiKeys.API_VERSIONS.latestVersion(),
                new ResponseHeaderData().setCorrelationId(1),
                new ApiVersionsResponseData().setApiKeys(v),
                filterContext);

        // Then
        Mockito.verify(filterContext).forwardResponse(any(), argCaptor.capture());
        Assertions.assertThat(((ApiVersionsResponseData) argCaptor.getValue()).apiKeys().find(ApiKeys.FETCH.id).maxVersion())
                .isEqualTo(RecordTransformationFilter.LATEST_FETCH_API_VERSION_USING_TOPIC_NAMES);
    }

    @Test
    void shouldPassThroughWhenNoMappers() {

        // Given
        when(filterContext.createByteBufferOutputStream(anyInt()))
                .thenReturn(new ByteBufferOutputStream(100));
        var filter = new RecordTransformationFilter(Map.of("my-topic", jsonTransformation(
                List.of(),
                List.of())
        ));
        var records = singletonBatch(
                "54".getBytes(StandardCharsets.UTF_8),
                "{}".getBytes(StandardCharsets.UTF_8)
        );

        // When
        filter.onFetchResponse(RecordTransformationFilter.LATEST_FETCH_API_VERSION_USING_TOPIC_NAMES,
                new ResponseHeaderData().setCorrelationId(42),
                new FetchResponseData().setErrorCode(Errors.NONE.code())
                        .setResponses(List.of(new FetchResponseData.FetchableTopicResponse()
                                .setTopic("my-topic")
                                .setPartitions(List.of(new FetchResponseData.PartitionData()
                                        .setPartitionIndex(78)
                                        .setErrorCode(Errors.NONE.code())
                                        .setRecords(records))))),
                filterContext);

        // Then
        Mockito.verify(filterContext).forwardResponse(any(), argCaptor.capture());
        List<FetchResponseData.FetchableTopicResponse> responses = ((FetchResponseData) argCaptor.getValue()).responses();
        KafkaAssertions.assertThat(assertSingletonPartitionNoErrors(responses))
                .hasNumBatches(1)
                .firstBatch()
                .firstRecord()
                .hasKeyEqualTo("54")
                .hasValueEqualTo("{}");
    }

    @Test
    void shouldPatchJsonRecordKey() {
        // Given
        when(filterContext.createByteBufferOutputStream(anyInt()))
                .thenReturn(new ByteBufferOutputStream(100));
        var filter = new RecordTransformationFilter(Map.of("my-topic", jsonTransformation(
                List.of(ApplyJsonPatchTest.ADD_ONE),
                List.of())
        ));
        var records = singletonBatch(
                "{}".getBytes(StandardCharsets.UTF_8),
                "54".getBytes(StandardCharsets.UTF_8)
        );

        // When
        filter.onFetchResponse(RecordTransformationFilter.LATEST_FETCH_API_VERSION_USING_TOPIC_NAMES,
                new ResponseHeaderData().setCorrelationId(42),
                new FetchResponseData().setErrorCode(Errors.NONE.code())
                        .setResponses(List.of(new FetchResponseData.FetchableTopicResponse()
                                .setTopic("my-topic")
                                .setPartitions(List.of(new FetchResponseData.PartitionData()
                                        .setPartitionIndex(78)
                                        .setErrorCode(Errors.NONE.code())
                                        .setRecords(records))))),
                filterContext);

        // Then
        Mockito.verify(filterContext).forwardResponse(any(), argCaptor.capture());
        List<FetchResponseData.FetchableTopicResponse> responses = ((FetchResponseData) argCaptor.getValue()).responses();
        KafkaAssertions.assertThat(assertSingletonPartitionNoErrors(responses))
                .hasNumBatches(1)
                .firstBatch()
                .firstRecord()
                .hasKeyEqualTo("{\"one\":1}")
                .hasValueEqualTo("54");
    }

    @Test
    void shouldPatchJsonRecordValue() {
        // Given
        when(filterContext.createByteBufferOutputStream(anyInt()))
                .thenReturn(new ByteBufferOutputStream(100));
        var filter = new RecordTransformationFilter(Map.of("my-topic", jsonTransformation(
                List.of(),
                List.of(ApplyJsonPatchTest.ADD_ONE))
        ));
        var records = singletonBatch(
                "54".getBytes(StandardCharsets.UTF_8),
                "{}".getBytes(StandardCharsets.UTF_8)
        );

        // When
        filter.onFetchResponse(RecordTransformationFilter.LATEST_FETCH_API_VERSION_USING_TOPIC_NAMES,
                new ResponseHeaderData().setCorrelationId(42),
                new FetchResponseData().setErrorCode(Errors.NONE.code())
                        .setResponses(List.of(new FetchResponseData.FetchableTopicResponse()
                                .setTopic("my-topic")
                                .setPartitions(List.of(new FetchResponseData.PartitionData()
                                        .setPartitionIndex(78)
                                        .setErrorCode(Errors.NONE.code())
                                        .setRecords(records))))),
                filterContext);

        // Then
        Mockito.verify(filterContext).forwardResponse(any(), argCaptor.capture());
        List<FetchResponseData.FetchableTopicResponse> responses = ((FetchResponseData) argCaptor.getValue()).responses();

        KafkaAssertions.assertThat(assertSingletonPartitionNoErrors(responses))
                .hasNumBatches(1)
                .firstBatch()
                .firstRecord()
                .hasKeyEqualTo("54")
                .hasValueEqualTo("{\"one\":1}");
    }

    @Test
    void shouldPassThroughBytesWhenNoMappers() {
        // Given
        when(filterContext.createByteBufferOutputStream(anyInt()))
                .thenReturn(new ByteBufferOutputStream(100));
        var filter = new RecordTransformationFilter(Map.of("my-topic", bytesTransformation(
                List.of(),
                List.of())
        ));
        var records = singletonBatch(
                new byte[] {1, 2, 4, 8},
                new byte[] {0, 1, 1, 2, 3, 5, 8}
        );

        // When
        filter.onFetchResponse(RecordTransformationFilter.LATEST_FETCH_API_VERSION_USING_TOPIC_NAMES,
                new ResponseHeaderData().setCorrelationId(42),
                new FetchResponseData().setErrorCode(Errors.NONE.code())
                        .setResponses(List.of(new FetchResponseData.FetchableTopicResponse()
                                .setTopic("my-topic")
                                .setPartitions(List.of(new FetchResponseData.PartitionData()
                                        .setPartitionIndex(78)
                                        .setErrorCode(Errors.NONE.code())
                                        .setRecords(records))))),
                filterContext);

        // Then
        Mockito.verify(filterContext).forwardResponse(any(), argCaptor.capture());
        List<FetchResponseData.FetchableTopicResponse> responses = ((FetchResponseData) argCaptor.getValue()).responses();

        KafkaAssertions.assertThat(assertSingletonPartitionNoErrors(responses))
                .hasNumBatches(1)
                .firstBatch()
                .firstRecord()
                .hasKeyEqualTo(new byte[] {1, 2, 4, 8})
                .hasValueEqualTo(new byte[] {0, 1, 1, 2, 3, 5, 8});
    }


}
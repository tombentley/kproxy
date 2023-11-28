/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.encryption;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

import org.apache.kafka.common.header.Header;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.apache.kafka.common.message.ProduceRequestData;
import org.apache.kafka.common.message.RequestHeaderData;
import org.apache.kafka.common.protocol.ApiMessage;
import org.apache.kafka.common.record.CompressionType;
import org.apache.kafka.common.record.MemoryRecordsBuilder;
import org.apache.kafka.common.record.Record;
import org.apache.kafka.common.record.RecordBatch;
import org.apache.kafka.common.record.TimestampType;
import org.apache.kafka.common.utils.ByteBufferOutputStream;
import org.assertj.core.api.AbstractAssert;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.assertj.core.matcher.AssertionMatcher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.hamcrest.MockitoHamcrest;
import org.mockito.junit.jupiter.MockitoExtension;

import io.kroxylicious.proxy.filter.FilterContext;
import io.kroxylicious.proxy.filter.RequestFilterResult;

import static io.kroxylicious.filter.encryption.ProduceRequestDataCondition.hasRecordsForTopic;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.Mock.Strictness.LENIENT;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EnvelopeEncryptionFilterTest {

    public static final String UNENCRYPTED_TOPIC = "unencrypted";
    public static final String ENCRYPTED_TOPIC = "encrypt_me";
    public static final String KEK_ID_1 = "KEK_ID_1";

    @Mock(strictness = LENIENT)
    KeyManager<String> keyManager;

    @Mock
    TopicNameBasedKekSelector<String> kekSelector;

    @Mock(strictness = LENIENT)
    private FilterContext context;

    @Captor
    private ArgumentCaptor<ApiMessage> apiMessageCaptor;

    private EnvelopeEncryptionFilter<String> encryptionFilter;

    @BeforeEach
    void setUp() {
        when(context.forwardRequest(any(RequestHeaderData.class), apiMessageCaptor.capture())).then(invocationOnMock -> {
            final RequestFilterResult filterResult = mock(RequestFilterResult.class);
            return CompletableFuture.completedFuture(filterResult);
        });

        when(context.createByteBufferOutputStream(anyInt())).thenAnswer(invocationOnMock -> {
            final int capacity = invocationOnMock.getArgument(0);
            return new ByteBufferOutputStream(capacity);
        });

        final Map<String, String> topicNameToKekId = new HashMap<>();
        topicNameToKekId.put(UNENCRYPTED_TOPIC, null);
        topicNameToKekId.put(ENCRYPTED_TOPIC, KEK_ID_1);
        when(kekSelector.selectKek(anySet())).thenReturn(CompletableFuture.completedFuture(topicNameToKekId));

        when(keyManager.encrypt(any(), any(), any())).thenAnswer(invocationOnMock -> {
            final List<? extends Record> actualRecords = invocationOnMock.getArgument(1);
            final Receiver receiver = invocationOnMock.getArgument(2);
            for (Record actualRecord : actualRecords) {
                receiver.accept(actualRecord, ByteBuffer.allocate(actualRecord.sizeInBytes()), new Header[0]);
            }
            return CompletableFuture.completedFuture(null);
        });

        encryptionFilter = new EnvelopeEncryptionFilter<>(keyManager, kekSelector);
    }

    @Test
    void shouldNotEncryptTopicWithoutKeyId() {
        // Given
        final ProduceRequestData produceRequestData = buildProduceRequestData(new Payload(UNENCRYPTED_TOPIC, "Hello World!"));

        // When
        encryptionFilter.onProduceRequest(ProduceRequestData.HIGHEST_SUPPORTED_VERSION, new RequestHeaderData(), produceRequestData, context);

        // Then
        verify(keyManager, times(0)).encrypt(any(), any(), any());
    }

    @Test
    void shouldEncryptTopicWithKeyId() {
        // Given
        final ProduceRequestData produceRequestData = buildProduceRequestData(new Payload(ENCRYPTED_TOPIC, "Hello World!"));

        // When
        encryptionFilter.onProduceRequest(ProduceRequestData.HIGHEST_SUPPORTED_VERSION, new RequestHeaderData(), produceRequestData, context);

        // Then
        verify(keyManager).encrypt(any(), any(), any());
    }

    @Test
    void shouldOnlyEncryptTopicWithKeyId() {
        // Given
        final Payload encryptedPayload = new Payload(ENCRYPTED_TOPIC, "Hello Ciphertext World!");
        final ProduceRequestData produceRequestData = buildProduceRequestData(encryptedPayload,
                new Payload(UNENCRYPTED_TOPIC, "Hello Plaintext World!"));

        // When
        encryptionFilter.onProduceRequest(ProduceRequestData.HIGHEST_SUPPORTED_VERSION, new RequestHeaderData(), produceRequestData, context);

        // Then
        verify(keyManager).encrypt(any(),
                argThat(records -> assertThat(records)
                        .hasSize(1)
                        .allSatisfy(record -> assertThat(record.value()).isEqualByComparingTo(encryptedPayload.messageBytes()))),
                any());
    }

    @Test
    void shouldEncryptTopic() {
        // Given
        final Payload encryptedPayload = new Payload(ENCRYPTED_TOPIC, "Hello Ciphertext World!");
        final ProduceRequestData produceRequestData = buildProduceRequestData(encryptedPayload,
                new Payload(UNENCRYPTED_TOPIC, "Hello Plaintext World!"));

        // When
        encryptionFilter.onProduceRequest(ProduceRequestData.HIGHEST_SUPPORTED_VERSION, new RequestHeaderData(), produceRequestData, context);

        // Then
        verify(context).forwardRequest(any(), argThat(request ->
                assertThat(request).isInstanceOf(ProduceRequestData.class)
                        .asInstanceOf(InstanceOfAssertFactories.type(ProduceRequestData.class))
                        .is(hasRecordsForTopic(ENCRYPTED_TOPIC))
        ));
    }

    private static ProduceRequestData buildProduceRequestData(Payload... payloads) {
        var requestData = new ProduceRequestData();

        // Build records from stream
        var stream = new ByteBufferOutputStream(ByteBuffer.allocate(1000));
        var topics = new ProduceRequestData.TopicProduceDataCollection();

        for (Payload payload : payloads) {
            var recordsBuilder = new MemoryRecordsBuilder(stream, RecordBatch.CURRENT_MAGIC_VALUE, CompressionType.NONE, TimestampType.CREATE_TIME, 0,
                    RecordBatch.NO_TIMESTAMP, RecordBatch.NO_PRODUCER_ID, RecordBatch.NO_PRODUCER_EPOCH, RecordBatch.NO_SEQUENCE, false, false,
                    RecordBatch.NO_PARTITION_LEADER_EPOCH, stream.remaining());

            // Create record Headers
            Header header = new RecordHeader("myKey", "myValue".getBytes());

            // Add transformValue as buffer to records
            recordsBuilder.append(RecordBatch.NO_TIMESTAMP, null, payload.messageBytes(), new Header[]{ header });
            var records = recordsBuilder.build();
            // Build partitions from built records
            var partitions = new ArrayList<ProduceRequestData.PartitionProduceData>();
            var partitionData = new ProduceRequestData.PartitionProduceData();
            partitionData.setRecords(records);
            partitions.add(partitionData);
            // Build topics from built partitions

            var topicData = new ProduceRequestData.TopicProduceData();
            topicData.setPartitionData(partitions);
            topicData.setName(payload.topicName);
            topics.add(topicData);
        }

        // Add built topics to ProduceRequestData object so that we can return it
        requestData.setTopicData(topics);
        return requestData;
    }

    private record Payload(String topicName, String message) {
        ByteBuffer messageBytes() {
            return ByteBuffer.wrap(message.getBytes(StandardCharsets.UTF_8)).position(0);
        }
    }

    public static <T> T argThat(Consumer<T> assertions) {
        return MockitoHamcrest.argThat(new AssertionMatcher<>() {

            String underlyingDescription;

            @Override
            public void assertion(T actual) throws AssertionError {
                AbstractAssert.setDescriptionConsumer(description -> underlyingDescription = description.value());
                assertions.accept(actual);
            }

            @Override
            public void describeTo(org.hamcrest.Description description) {
                super.describeTo(description);
                description.appendValue(Objects.requireNonNullElse(underlyingDescription, "custom argument matcher"));
            }
        });
    }
}
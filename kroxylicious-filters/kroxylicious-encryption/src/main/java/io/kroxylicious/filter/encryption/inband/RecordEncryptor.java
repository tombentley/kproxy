/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.encryption.inband;

import java.nio.ByteBuffer;
import java.util.Objects;

import org.apache.kafka.common.header.Header;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.apache.kafka.common.record.Record;
import org.apache.kafka.common.record.RecordBatch;

import io.kroxylicious.filter.encryption.EncryptionScheme;
import io.kroxylicious.filter.encryption.EncryptionVersion;
import io.kroxylicious.filter.encryption.RecordField;
import io.kroxylicious.filter.encryption.dek.BufferTooSmallException;
import io.kroxylicious.filter.encryption.dek.Dek;
import io.kroxylicious.filter.encryption.records.RecordTransform;
import io.kroxylicious.kms.service.Serde;

import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.Nullable;

/**
 * A {@link RecordTransform} that encrypts records so that they can be later decrypted by {@link RecordDecryptor}.
 * @param <K> The type of KEK id
 */
class RecordEncryptor<K, E> implements RecordTransform<Dek<E>.Encryptor> {

    /**
     * The encryption header. The value is the encryption version that was used to serialize the parcel and the wrapper.
     */
    static final String ENCRYPTION_HEADER_NAME = "kroxylicious.io/encryption";
    private final EncryptionVersion encryptionVersion;
    private final EncryptionScheme<K> encryptionScheme;
    private final Serde<E> edekSerde;
    private final String topicName;
    private final int partition;
    private Dek<E>.Encryptor encryptor;
    private final ByteBuffer recordBuffer;
    /**
     * The encryption version used on the produce path.
     * Note that the encryption version used on the fetch path is read from the
     * {@link #ENCRYPTION_HEADER_NAME} header.
     */
    private final Header[] encryptionHeader;
    private @Nullable ByteBuffer transformedValue;
    private @Nullable Header[] transformedHeaders;
    private RecordBatch batch;

    /**
     * Constructor (obviously).
     * @param encryptionVersion The encryption version
     * @param encryptionScheme The encryption scheme for this key
     * @param edekSerde Serde for the encrypted DEK.
     * @param recordBuffer A buffer
     */
    RecordEncryptor(@NonNull String topicName,
                    int partition,
                    @NonNull EncryptionVersion encryptionVersion,
                    @NonNull EncryptionScheme<K> encryptionScheme,
                    @NonNull Serde<E> edekSerde,
                    @NonNull ByteBuffer recordBuffer) {
        this.topicName = Objects.requireNonNull(topicName);
        this.partition = partition;
        this.encryptionVersion = Objects.requireNonNull(encryptionVersion);
        this.encryptionScheme = Objects.requireNonNull(encryptionScheme);
        this.edekSerde = Objects.requireNonNull(edekSerde);
        this.recordBuffer = Objects.requireNonNull(recordBuffer);
        this.encryptionHeader = new Header[]{ new RecordHeader(ENCRYPTION_HEADER_NAME, new byte[]{ encryptionVersion.code() }) };
    }

    @Override
    public void initBatch(@NonNull RecordBatch batch) {
        this.batch = Objects.requireNonNull(batch);
    }

    @Override
    public void resetAfterTransform(Dek<E>.Encryptor encryptor, Record record) {
        recordBuffer.clear();
    }

    @Override
    public void init(Dek<E>.Encryptor encryptor, @NonNull Record kafkaRecord) throws BufferTooSmallException {
        if (this.batch == null) {
            throw new IllegalStateException();
        }
        if (encryptionScheme.recordFields().contains(RecordField.RECORD_HEADER_VALUES)
                && kafkaRecord.headers().length > 0
                && !kafkaRecord.hasValue()) {
            // todo implement header encryption preserving null record-values
            throw new IllegalStateException("encrypting headers prohibited when original record value null, we must preserve the null for tombstoning");
        }
        this.encryptor = Objects.requireNonNull(encryptor);
        this.transformedValue = doTransformValue(kafkaRecord);
        this.transformedHeaders = doTransformHeaders(kafkaRecord);
    }

    @Nullable
    private ByteBuffer doTransformValue(@NonNull Record kafkaRecord) throws BufferTooSmallException {
        final ByteBuffer transformedValue;
        if (kafkaRecord.hasValue()) {
            transformedValue = writeWrapper(kafkaRecord, recordBuffer);
        }
        else {
            transformedValue = null;
        }
        return transformedValue;
    }

    private Header[] doTransformHeaders(@NonNull Record kafkaRecord) {
        final Header[] transformedHeaders;
        if (kafkaRecord.hasValue()) {
            Header[] oldHeaders = kafkaRecord.headers();
            if (encryptionScheme.recordFields().contains(RecordField.RECORD_HEADER_VALUES) || oldHeaders.length == 0) {
                transformedHeaders = encryptionHeader;
            }
            else {
                transformedHeaders = new Header[1 + oldHeaders.length];
                transformedHeaders[0] = encryptionHeader[0];
                System.arraycopy(oldHeaders, 0, transformedHeaders, 1, oldHeaders.length);
            }
        }
        else {
            transformedHeaders = kafkaRecord.headers();
        }
        return transformedHeaders;
    }

    @Nullable
    private ByteBuffer writeWrapper(@NonNull Record kafkaRecord,
                                    @NonNull ByteBuffer buffer)
            throws BufferTooSmallException {
        encryptionVersion.wrapperVersion().writeWrapper(edekSerde,
                Objects.requireNonNull(encryptor.edek()),
                topicName,
                partition,
                batch,
                kafkaRecord,
                encryptor,
                encryptionVersion.parcelVersion(),
                encryptionScheme.aadSpec(),
                encryptionScheme.recordFields(),
                buffer);
        recordBuffer.flip();
        return recordBuffer;
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
    public @Nullable ByteBuffer transformKey(Record record) {
        return record.key();
    }

    @Override
    public @Nullable ByteBuffer transformValue(Record kafkaRecord) {
        return transformedValue == null ? null : transformedValue.duplicate();
    }

    @Override
    public @Nullable Header[] transformHeaders(Record kafkaRecord) {
        return transformedHeaders;
    }

}

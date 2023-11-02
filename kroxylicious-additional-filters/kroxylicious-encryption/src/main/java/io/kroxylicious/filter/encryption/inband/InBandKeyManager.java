/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.encryption.inband;

import java.nio.ByteBuffer;
import java.security.SecureRandom;
import java.util.concurrent.CompletionStage;
import java.util.stream.Stream;

import org.apache.kafka.common.record.Record;

import io.kroxylicious.filter.encryption.KeyManager;
import io.kroxylicious.filter.encryption.Receiver;
import io.kroxylicious.filter.encryption.RecordEncryptionRequest;
import io.kroxylicious.kms.service.Kms;
import io.kroxylicious.kms.service.Serde;

import edu.umd.cs.findbugs.annotations.NonNull;

/**
 * An implementation of {@link KeyManager} that uses envelope encryption, AES-GCM and stores the KEK id and encrypted DEK
 * alongside the record ("in-band").
 * @param <K> The type of KEK id.
 * @param <E> The type of the encrypted DEK.
 */
public class InBandKeyManager<K, E> implements KeyManager<K> {

    private final Kms<K, E> kms;
    private final BufferPool bufferPool;
    private final Serde<K> kekIdSerde;
    private final Serde<E> edekSerde;

    public InBandKeyManager(Kms<K, E> kms, BufferPool bufferPool) {
        this.kms = kms;
        this.bufferPool = bufferPool;
        this.edekSerde = kms.edekSerde();
        this.kekIdSerde = kms.keyIdSerde();
    }

    private CompletionStage<DekContext<K>> currentDekContext(@NonNull K kekId) {
        // TODO caching with expiry
        // TODO count the number of encryptions
        // TODO destroy DEK material
        return kms.generateDekPair(kekId)
                .thenApply(dekPair -> {
                    E edek = dekPair.edek();
                    short kekIdSize = (short) kekIdSerde.sizeOf(kekId);
                    short edekSize = (short) edekSerde.sizeOf(edek);
                    // TODO use buffer pool
                    ByteBuffer prefix = ByteBuffer.allocate(
                            Short.BYTES + // kekId size
                                    kekIdSize + // the kekId
                                    Short.BYTES + // DEK size
                                    edekSize); // the DEK
                    prefix.putShort(kekIdSize);
                    kekIdSerde.serialize(kekId, prefix);
                    prefix.putShort(edekSize);
                    edekSerde.serialize(edek, prefix);
                    prefix.flip();

                    return new DekContext<>(kekId, prefix,
                            new AesGcmEncryptor(new AesGcmIvGenerator(new SecureRandom()), dekPair.dek()));
                });
    }

    @NonNull
    @Override
    public CompletionStage<Void> encrypt(@NonNull K kekId,
                                         @NonNull Stream<RecordEncryptionRequest> recordRequests,
                                         @NonNull Receiver receiver) {
        return currentDekContext(kekId).thenAccept(dekContext -> {
            recordRequests.forEach(recordRequest -> {
                // XXX accumulator
                var output = bufferPool.acquire(dekContext.encodedSize(recordRequest.plaintextSize()));
                try {
                    // TODO encrypt null values
                    dekContext.encode(recordRequest.plaintext(), output);
                    output.flip();
                    receiver.accept(output, recordRequest.kafkaRecord());
                }
                finally {
                    bufferPool.release(output);
                }
            });
        });
    }

    private CompletionStage<AesGcmEncryptor> resolve(@NonNull ByteBuffer buffer) {
        // Read the prefix
        var kekLength = buffer.getShort();
        int origLimit = buffer.limit();
        buffer.limit(buffer.position() + kekLength);
        var kekId = kekIdSerde.deserialize(buffer);
        buffer.limit(origLimit);
        var edekLength = buffer.getShort();
        buffer.limit(buffer.position() + edekLength);
        var edek = edekSerde.deserialize(buffer);
        buffer.limit(origLimit);

        return kms.decryptEdek(kekId, edek)
                .thenApply(dek -> new AesGcmEncryptor(new AesGcmIvGenerator(new SecureRandom()), dek));
    }

    @NonNull
    @Override
    public CompletionStage<Void> decrypt(@NonNull Record kafkaRecord,
                                         @NonNull Receiver receiver) {
        ByteBuffer value = kafkaRecord.value();
        return resolve(value).thenApply(encryptor -> {
            ByteBuffer output = null;
            try {
                int plaintextSize = kafkaRecord.valueSize();
                // TODO ^^ this is an overestimate!
                // TODO leverage the fact that Cipher#doFinal is "copy safe"
                output = bufferPool.acquire(plaintextSize);
                encryptor.decrypt(value, output);
                output.flip();
                receiver.accept(output, kafkaRecord);
                return null;
            }
            finally {
                bufferPool.release(output);
            }
        });
    }
}

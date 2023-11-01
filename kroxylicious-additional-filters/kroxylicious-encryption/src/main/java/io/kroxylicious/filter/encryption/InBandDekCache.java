/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.encryption;

import java.nio.ByteBuffer;
import java.security.SecureRandom;
import java.util.concurrent.CompletionStage;

import io.kroxylicious.kms.service.De;
import io.kroxylicious.kms.service.Kms;
import io.kroxylicious.kms.service.Ser;

class InBandDekCache<K, E> implements DekCache<K, E> {

    private final Kms<K, E> kms;
    private final Ser<K> kekIdSerializer;
    private final De<K> kekIdDeserializer;
    private final Ser<E> edekSerializer;
    private final De<E> edekDeserializer;

    InBandDekCache(Kms<K, E> kms) {
        this.kms = kms;
        this.edekSerializer = kms.edekSerializer();
        this.kekIdSerializer = kms.keyIdSerializer();
        this.edekDeserializer = kms.edekDeserializer();
        this.kekIdDeserializer = kms.keyIdDeserializer();
    }

    @Override
    public CompletionStage<DekContext<K>> forKekId(K kekId) {
        return kms.generateDekPair(kekId)
                .thenApply(dekPair -> {
                    E edek = dekPair.edek();
                    short kekIdSize = (short) kekIdSerializer.sizeOf(kekId);
                    short edekSize = (short) edekSerializer.sizeOf(edek);
                    // TODO use buffer pool?
                    ByteBuffer prefix = ByteBuffer.allocate(
                            Short.BYTES + // kekId size
                                    kekIdSize + // the kekId
                                    Short.BYTES + // DEK size
                                    edekSize); // the DEK
                    prefix.putShort(kekIdSize);
                    kekIdSerializer.serialize(kekId, prefix);
                    prefix.putShort(edekSize);
                    edekSerializer.serialize(edek, prefix);
                    prefix.flip();

                    var ivGenerator = new AesGcmIvGenerator(new SecureRandom());
                    return new DekContext<>(kekId, prefix,
                            new AesGcmEncryptor(ivGenerator, dekPair.dek()));
                });
    }

    @Override
    public CompletionStage<AesGcmEncryptor> resolve(ByteBuffer buffer) {
        // Read the prefix
        var kekLength = buffer.getShort();
        int origLimit = buffer.limit();
        buffer.limit(buffer.position() + kekLength);
        var kekId = kekIdDeserializer.deserialize(buffer);
        buffer.limit(origLimit);
        var edekLength = buffer.getShort();
        buffer.limit(buffer.position() + edekLength);
        var edek = edekDeserializer.deserialize(buffer);
        buffer.limit(origLimit);

        return kms.decryptEdek(kekId, edek)
                .thenApply(dek -> new AesGcmEncryptor(new AesGcmIvGenerator(new SecureRandom()), dek));
    }
}

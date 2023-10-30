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

public class InBandDekCache<K, E> implements DekCache<K, E> {

    private final Kms<K, E> kms;
    private final Ser<K> kekIdSerializer;
    private final De<K> kekIdDeserializer;
    private final Ser<E> edekSerializer;
    private final De<E> edekDeserializer;

    public InBandDekCache(Kms<K, E> kms) {
        this.kms = kms;
        this.edekSerializer = kms.edekSerializer();
        this.kekIdSerializer = kms.keyRefSerializer();
        this.edekDeserializer = kms.edekDeserializer();
        this.kekIdDeserializer = kms.keyRefDeserializer();
    }

    @Override
    public CompletionStage<DekContext<K>> forKekId(K kekId) {
        return kms.generateDekPair(kekId)
                .thenApply(dekPair -> {
                    E edek = dekPair.edek();
                    ByteBuffer prefix = ByteBuffer.allocate(Short.BYTES +
                            kekIdSerializer.sizeOf(kekId) +
                            Short.BYTES +
                            edekSerializer.sizeOf(edek));
                    prefix.putShort((short) kekIdSerializer.sizeOf(kekId));
                    kekIdSerializer.serialize(kekId, prefix);
                    prefix.putShort((short) edekSerializer.sizeOf(edek));
                    edekSerializer.serialize(edek, prefix);
                    prefix.flip();

                    var ivGenerator = new AesGcmIvGenerator(new SecureRandom());
                    return new DekContext<>(kekId, prefix,
                            new AesGcmEncryptor(ivGenerator, dekPair.dek()));
                });
    }

    @Override
    public CompletionStage<DekContext<K>> resolve(ByteBuffer buffer) {
        // Read the prefix
        var kekLength = buffer.getShort();
        buffer.limit(buffer.position() + kekLength);
        var kekId = kekIdDeserializer.deserialize(buffer);
        var edekLength = buffer.getShort();
        buffer.limit(buffer.position() + edekLength);
        var edek = edekDeserializer.deserialize(buffer);

        var f = kms.decryptEdek(kekId, edek);
        return f.thenApply(dek -> {
            return new DekContext<>(kekId, null,
                    new AesGcmEncryptor(new AesGcmIvGenerator(new SecureRandom()), dek));
        });
    }
}

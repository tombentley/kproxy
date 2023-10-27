/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.encryption.coordinator;

import io.kroxylicious.filter.encryption.AesGcmEncryptor;
import io.kroxylicious.filter.encryption.DekCache;
import io.kroxylicious.filter.encryption.Ser;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class InBandDekCache<K, E> implements DekCache<K, InBandDekCache.X> {

    record X<K, E>(K kekId, E edek) {

    }
    @Override
    public Ser<X> serializer() {
        return null;
    }

    @Override
    public CompletableFuture<Map<String, Map.Entry<X, AesGcmEncryptor>>> encryptors(Map<String, K> keks) {
        // TODO just ask the KMS for some more DEKs
        return null;
    }
}

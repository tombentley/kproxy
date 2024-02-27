/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.encryption;

import io.kroxylicious.filter.encryption.dek.DekManager;
import io.kroxylicious.filter.encryption.inband.DecryptionDekCache;
import io.kroxylicious.filter.encryption.inband.EncryptionDekCache;
import io.kroxylicious.kms.service.Kms;

/**
 * Things which are shared between instances of the filter.
 * Because they're shared between filter instances, the things shared here must be thread-safe.
 * @param <K> The type of KEK id.
 * @param <E> The type of the encrypted DEK.
 */
public class SharedEncryptionContext<K, E> {
    private final Kms<K, E> kms;
    private final RecordEncryption.Config configuration;
    private final DekManager<K, E> dekManager;
    private final EncryptionDekCache<K, E> encryptionDekCache;
    private final DecryptionDekCache<K, E> decryptionDekCache;

    /**
     * @param kms
     * @param configuration
     * @param dekManager
     * @param encryptionDekCache
     */
    SharedEncryptionContext(
                            Kms<K, E> kms,
                            RecordEncryption.Config configuration,
                            DekManager<K, E> dekManager,
                            EncryptionDekCache<K, E> encryptionDekCache,
                            DecryptionDekCache<K, E> decryptionDekCache) {
        this.kms = kms;
        this.configuration = configuration;
        this.dekManager = dekManager;
        this.encryptionDekCache = encryptionDekCache;
        this.decryptionDekCache = decryptionDekCache;
    }

    public Kms<K, E> kms() {
        return kms;
    }

    public RecordEncryption.Config configuration() {
        return configuration;
    }

    public DekManager<K, E> dekManager() {
        return dekManager;
    }

    public EncryptionDekCache<K, E> encryptionDekCache() {
        return encryptionDekCache;
    }

    public DecryptionDekCache<K, E> decryptionDekCache() {
        return decryptionDekCache;
    }
}

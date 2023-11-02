/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.encryption;

import java.util.Map;
import java.util.UUID;

import javax.crypto.SecretKey;

import io.kroxylicious.filter.encryption.inband.BufferPool;
import io.kroxylicious.filter.encryption.inband.InBandKeyManager;
import io.kroxylicious.kms.provider.kroxylicious.inmemory.InMemoryKmsService;
import io.kroxylicious.proxy.filter.FilterCreationContext;
import io.kroxylicious.proxy.filter.FilterFactory;

/**
 * A {@link FilterFactory} for {@link EnvelopeEncryptionFilter}.
 */
public class EnvelopeEncryption<K, E> implements FilterFactory<EnvelopeEncryptionFilter<K>, EnvelopeEncryption.Config> {

    private final InMemoryKmsService kmsService;

    record Config(
                  String kms,
                  Object kmsConfig,

                  Map<UUID, SecretKey> keys, /* Temporary - these fields will move */
                  Map<String, UUID> aliases,
                  String selectorTemplate) {

    }

    @Override
    public Class<EnvelopeEncryptionFilter<K>> filterType() {
        return (Class) EnvelopeEncryptionFilter.class;
    }

    @Override
    public Class<Config> configType() {
        return Config.class;
    }

    public EnvelopeEncryption() {
        this.kmsService = InMemoryKmsService.newInstance();
    }

    @Override
    public EnvelopeEncryptionFilter<K> createFilter(FilterCreationContext context, Config configuration) {
        // Replace with nested factories stuff
        var kms = kmsService.buildKms(new InMemoryKmsService.Config(12, 128, configuration.keys(), configuration.aliases()));

        var dk = new InBandKeyManager<>(kms, BufferPool.allocating());
        var kekSelector = new TemplateKekSelector<>(kms, configuration.selectorTemplate());
        // TODO validation of generics
        return (EnvelopeEncryptionFilter<K>) new EnvelopeEncryptionFilter<>(dk, kekSelector);
    }
}

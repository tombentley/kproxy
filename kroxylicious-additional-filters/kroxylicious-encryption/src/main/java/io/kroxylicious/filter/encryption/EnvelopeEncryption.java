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
import io.kroxylicious.proxy.filter.FilterFactory;
import io.kroxylicious.proxy.filter.FilterFactoryContext;
import io.kroxylicious.proxy.plugin.Plugin;
import io.kroxylicious.proxy.plugin.PluginConfigurationException;

import edu.umd.cs.findbugs.annotations.NonNull;

/**
 * A {@link FilterFactory} for {@link EnvelopeEncryptionFilter}.
 */
@Plugin(configType = EnvelopeEncryption.Config.class)
public class EnvelopeEncryption<K, E> implements FilterFactory<EnvelopeEncryption.Config, EnvelopeEncryption.Config> {

    private final InMemoryKmsService kmsService;

    record Config(
                  String kms,
                  Object kmsConfig,

                  Map<UUID, SecretKey> keys, /* Temporary - these fields will move */
                  Map<String, UUID> aliases,
                  String selectorTemplate) {

    }

    public EnvelopeEncryption() {
        this.kmsService = InMemoryKmsService.newInstance();
    }

    @Override
    public Config initialize(FilterFactoryContext context, Config config) throws PluginConfigurationException {
        return config;
    }

    @NonNull
    @Override
    public EnvelopeEncryptionFilter<K> createFilter(FilterFactoryContext context, Config configuration) {
        // Replace with nested factories stuff
        var kms = kmsService.buildKms(new InMemoryKmsService.Config(12, 128, configuration.keys(), configuration.aliases()));

        var dk = new InBandKeyManager<>(kms, BufferPool.allocating());
        var kekSelector = new TemplateKekSelector<>(kms, configuration.selectorTemplate());
        // TODO validation of generics
        return (EnvelopeEncryptionFilter<K>) new EnvelopeEncryptionFilter<>(dk, kekSelector);
    }
}

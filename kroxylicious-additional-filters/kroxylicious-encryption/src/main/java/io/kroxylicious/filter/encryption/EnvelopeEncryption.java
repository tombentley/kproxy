/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.encryption;

import io.kroxylicious.kms.provider.kroxylicious.inmemory.InMemoryKmsService;
import io.kroxylicious.proxy.filter.FilterCreationContext;
import io.kroxylicious.proxy.filter.FilterFactory;

/**
 * A {@link FilterFactory} for {@link EnvelopeEncryptionFilter}.
 */
public class EnvelopeEncryption<K, E> implements FilterFactory<EnvelopeEncryptionFilter<K, E>, EnvelopeEncryption.Config> {

    record Config(
                  String kms,
                  Object kmsConfig) {

    }

    @Override
    public Class<EnvelopeEncryptionFilter<K, E>> filterType() {
        return (Class) EnvelopeEncryptionFilter.class;
    }

    @Override
    public Class<Config> configType() {
        return Config.class;
    }

    @Override
    public EnvelopeEncryptionFilter<K, E> createFilter(FilterCreationContext context, Config configuration) {
        // Replace with nested factories stuff
        var kmsService = new InMemoryKmsService();
        var kms = kmsService.buildKms(new InMemoryKmsService.Config(12, 128));

        // More temporary code
        var key = kms.generateKey();
        kms.createAlias(key, "all");

        var dk = new InBandDekCache<>(kms);
        var kekSelector = new TemplateKekSelector<>(kms, "all");
        // TODO validation of generics
        return (EnvelopeEncryptionFilter<K, E>) new EnvelopeEncryptionFilter<>(dk, kekSelector);
    }
}

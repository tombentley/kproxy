/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.encryption;

import io.kroxylicious.filter.encryption.coordinator.CoordinatedDekCache;
import io.kroxylicious.kms.service.Kms;
import io.kroxylicious.kms.service.KmsService;
import io.kroxylicious.proxy.filter.FilterCreationContext;
import io.kroxylicious.proxy.filter.FilterFactory;

/**
 * A {@link FilterFactory} for {@link EnvelopeEncryptionFilter}.
 */
public class EnvelopeEncryption implements FilterFactory<EnvelopeEncryptionFilter, EnvelopeEncryption.Config> {

    static record Config(
                         String kms,
                         Object kmsConfig) {

    }

    @Override
    public Class<EnvelopeEncryptionFilter> filterType() {
        return EnvelopeEncryptionFilter.class;
    }

    @Override
    public Class<Config> configType() {
        return Config.class;
    }

    @Override
    public EnvelopeEncryptionFilter<?, ?> createFilter(FilterCreationContext context, Config configuration) {
        KmsService<Object, ?, ?> kms = null;
        Kms<?, ?> kms1 = kms.buildKms(null);
        DekCache dk = (DekCache) CoordinatedDekCache.build(kms1, "", "");
        TopicNameBasedKekSelector kekSelector = null;
        return new EnvelopeEncryptionFilter(dk, kekSelector);
    }
}

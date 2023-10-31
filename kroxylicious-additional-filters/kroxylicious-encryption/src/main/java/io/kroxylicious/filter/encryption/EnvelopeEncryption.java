/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.encryption;

import io.kroxylicious.kms.service.Kms;
import io.kroxylicious.kms.service.KmsService;
import io.kroxylicious.proxy.filter.FilterCreationContext;
import io.kroxylicious.proxy.filter.FilterFactory;

/**
 * A {@link FilterFactory} for {@link EnvelopeEncryptionFilter}.
 */
public class EnvelopeEncryption<K, E> implements FilterFactory<EnvelopeEncryptionFilter<K, E>, EnvelopeEncryption.Config> {

    static record Config(
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
        KmsService<Object, K, E> kmsService = null;
        Kms<K, E> kms = kmsService.buildKms(null);
        DekCache<K, E> dk = new InBandDekCache<>(kms);
        TopicNameBasedKekSelector<K> kekSelector = new TemplateKekSelector<>(kms, "topic-${topicName}");
        // TODO validation of generics
        return new EnvelopeEncryptionFilter<>(dk, kekSelector);
    }
}

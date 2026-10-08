/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.record.manipulation.filter.apicurio;

import java.util.Objects;

import io.kroxylicious.filter.record.manipulation.filter.service.ServiceFactory;

public class SchemaRegistryFactory implements ServiceFactory<ApicurioRegistryClientConfig, SchemaRegistryClient> {
    @Override
    public SchemaRegistryClient create(ApicurioRegistryClientConfig config) {
        Objects.requireNonNull(config, "ApicurioRegistryClientConfig must not be null");
        return new ApicurioSchemaRegistryClient(config.registryUrl(), config.timeout(), config.tls());
    }
}

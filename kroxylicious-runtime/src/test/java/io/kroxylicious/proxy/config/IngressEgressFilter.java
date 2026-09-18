/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.proxy.config;

import java.net.InetSocketAddress;
import java.net.URI;

import io.kroxylicious.proxy.filter.Filter;
import io.kroxylicious.proxy.filter.FilterFactory;
import io.kroxylicious.proxy.filter.FilterFactoryContext;
import io.kroxylicious.proxy.plugin.PluginConfigurationException;

public class IngressEgressFilter implements FilterFactory<IngressEgressFilter.Config, Void> {
    @Override
    public Void initialize(FilterFactoryContext context, Config config) throws PluginConfigurationException {
        return null;
    }

    @Override
    public Filter createFilter(FilterFactoryContext context, Void initializationData) {
        return null;
    }

    public record Config(
            URI fooUrl,
            InetSocketAddress addr
    ) {}
}

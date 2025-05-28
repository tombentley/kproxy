/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.proxy.router;

import java.util.Set;

import io.kroxylicious.proxy.plugin.PluginConfigurationException;

import edu.umd.cs.findbugs.annotations.NonNull;

public interface RouterFactory<C, I> {


    I initialize(RouterFactoryContext context, C config,
                 Set<String> routes) throws PluginConfigurationException;

    @NonNull
    Router createRouter(RouterFactoryContext context, I initializationData);

    default void close(I initializationData) {
    }
}


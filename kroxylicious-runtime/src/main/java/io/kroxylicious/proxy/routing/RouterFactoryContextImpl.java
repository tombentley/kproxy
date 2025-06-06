/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.proxy.routing;

import io.kroxylicious.proxy.config.PluginFactoryRegistry;
import io.kroxylicious.proxy.config.ServiceBasedPluginFactoryRegistry;
import io.kroxylicious.proxy.filter.FilterDispatchExecutor;
import io.kroxylicious.proxy.router.RouterFactoryContext;

import edu.umd.cs.findbugs.annotations.NonNull;

public class RouterFactoryContextImpl implements RouterFactoryContext {
    private final PluginFactoryRegistry pfr;
    private final FilterDispatchExecutor executor;

    RouterFactoryContextImpl(PluginFactoryRegistry pfr, FilterDispatchExecutor executor) {
        this.pfr = pfr;
        this.executor = executor;
    }

    @Override
    public FilterDispatchExecutor dispatchExecutor() {
        return executor;
    }

    @Override
    public <P> @NonNull P pluginInstance(@NonNull Class<P> pluginClass, @NonNull String instanceName) {
        return pfr.pluginFactory(pluginClass).pluginInstance(instanceName);
    }
}

/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.proxy.bootstrap;

import io.kroxylicious.proxy.config.Configuration;
import io.kroxylicious.proxy.config.PluginFactoryRegistry;
import io.kroxylicious.proxy.config.VirtualCluster;
import io.kroxylicious.proxy.model.VirtualClusterModel;

public class TopologyFactory {

    private final Configuration configuration;
    private final PluginFactoryRegistry pfr;

    TopologyFactory(PluginFactoryRegistry pfr, Configuration configuration) {
        this.configuration = configuration;
    }


    Handler makeAThing(VirtualCluster virtualCluster) {
        // TODO get the router
        RouterFactory routerFactory = pfr.pluginFactory(RouterFactory.class).pluginInstance(routerName);
        routerFactory.init();
        virtualCluster.

    }
}

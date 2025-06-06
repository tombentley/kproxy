/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.proxy.routing;

import io.kroxylicious.proxy.config.RouteDefinition;
import io.kroxylicious.proxy.config.RouterDefinition;
import io.kroxylicious.proxy.router.Router;
import io.kroxylicious.proxy.router.RouterFactory;
import io.kroxylicious.proxy.router.RouterFactoryContext;

record InitializedRouterFactory<I>(
        RouterDefinition routerDefinition,
        RouterFactoryContext context,
        RouterFactory<?, I> routerFactory,
        I initializationData
) {
    Router create() {
        return routerFactory.createRouter(context, initializationData);
    }

    public RouteDefinition route(String routeName) {
        return TopologyFactory.stream(routerDefinition.routes()).filter(routeDefinition -> routeDefinition.routeName().equals(routeName)).findFirst().orElse(null);
    }

    // TODO arrange to close the routerFactory
}

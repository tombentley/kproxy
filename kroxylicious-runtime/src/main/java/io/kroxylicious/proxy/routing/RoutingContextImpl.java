/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.proxy.routing;

import io.kroxylicious.proxy.router.RoutingContext;
import io.kroxylicious.proxy.router.RoutingResultBuilder;

public class RoutingContextImpl implements RoutingContext {
    @Override
    public RoutingResultBuilder routingResultBuilder() {
        return new RoutingResultBuilderImpl();
    }
}

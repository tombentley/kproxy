/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.proxy.router;

import java.util.Set;

import org.apache.kafka.common.message.ResponseHeaderData;
import org.apache.kafka.common.protocol.ApiKeys;
import org.apache.kafka.common.protocol.ApiMessage;

import io.kroxylicious.proxy.filter.FilterContext;

public interface RoutingResultBuilder {

    /**
     * Forward the request on the given route
     * (that was previously configured via {@link RouterFactory#initialize(RouterFactoryContext, Object, Set)}).
     * @param route The route on which to forward the request
     */
    RoutingResultBuilder forwardTo(String route);

    /**
     * Send a response to the client.
     */
    RoutingResultBuilder respondWith(ApiKeys apiKey,
                                     ResponseHeaderData header,
                                     ApiMessage response,
                                     FilterContext context);

    /**
     * Disconnect from the client, tearing down any connection to a broker.
     */
    TerminalRoutingResultBuilder disconnect();
}

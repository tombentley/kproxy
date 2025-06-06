/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.proxy.routing;

import org.apache.kafka.common.message.ResponseHeaderData;
import org.apache.kafka.common.protocol.ApiKeys;
import org.apache.kafka.common.protocol.ApiMessage;

import io.kroxylicious.proxy.filter.FilterContext;
import io.kroxylicious.proxy.router.RoutingResult;
import io.kroxylicious.proxy.router.RoutingResultBuilder;
import io.kroxylicious.proxy.router.TerminalRoutingResultBuilder;

public class RoutingResultBuilderImpl implements RoutingResultBuilder {
    @Override
    public RoutingResultBuilder forwardTo(String route) {
        return new ForwardRoutingResult(route);
    }

    @Override
    public RoutingResultBuilder respondWith(ApiKeys apiKey, ResponseHeaderData header, ApiMessage response, FilterContext context) {
        return new RespondRoutingResult();
    }

    @Override
    public TerminalRoutingResultBuilder disconnect() {
        return new DisconnectRoutingResult();
    }

    public static record ForwardRoutingResult(String route) implements RoutingResult {}

    public static record DisconnectRoutingResult() implements RoutingResult {}

    public static record RespondRoutingResult() implements RoutingResult {}

    public static record MakeRequestRoutingResult() implements RoutingResult {}
}

/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.kroxylicious.proxy.internal.filter;

import io.kroxylicious.proxy.bootstrap.FilterChainFactory;
import io.kroxylicious.proxy.filter.NetFilter;

public class SimpleNetFilter implements NetFilter {

    private final String remoteHost;
    private final int remotePort;
    private final FilterChainFactory filterChainFactory;
    private final boolean logNetwork;
    private final boolean logFrames;

    public SimpleNetFilter(String remoteHost, int remotePort, FilterChainFactory filterChainFactory, boolean logNetwork, boolean logFrames) {
        this.remoteHost = remoteHost;
        this.remotePort = remotePort;
        this.filterChainFactory = filterChainFactory;
        this.logNetwork = logNetwork;
        this.logFrames = logFrames;
    }

    @Override
    public void upstreamBroker(NetFilterContext context) {
        context.connect(remoteHost, remotePort, filterChainFactory.createFilters());
    }
}

/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.kroxylicious.proxy.cluster;

import static java.util.Objects.requireNonNull;

/**
 * Network endpoint for reaching a proxy cluster member.
 *
 * @param host the hostname or IP address
 * @param port the port number
 * @param transport the transport protocol
 */
public record ProxyClusterEndpoint(
                                   String host,
                                   int port,
                                   Transport transport) {

    /** Validates the endpoint parameters. */
    public ProxyClusterEndpoint {
        requireNonNull(host, "host");
        if (host.isBlank()) {
            throw new IllegalArgumentException("host must not be blank");
        }
        if (port < 1 || port > 65535) {
            throw new IllegalArgumentException("port must be between 1 and 65535, got " + port);
        }
        requireNonNull(transport, "transport");
    }
}

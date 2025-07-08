/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.proxy.authentication;

/**
 * Implemented by a {@link io.kroxylicious.proxy.filter.Filter} that provides
 * the credentials for the TLS connection between the proxy and the Kafka server.
 */
public interface ServerTlsCredentialSupplier {
    /**
     * Return the TlsCredentials for the connection.
     * @param context The context.
     * @return the TlsCredentials for the connection.
     */
    TlsCredentials tlsCredentials(ServerTlsCredentialContext context);
}

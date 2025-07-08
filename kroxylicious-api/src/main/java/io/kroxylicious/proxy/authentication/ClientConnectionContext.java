/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.proxy.authentication;

import java.security.cert.X509Certificate;

import edu.umd.cs.findbugs.annotations.Nullable;

/**
 * The context API for {@link ClientConnectionAware}.
 * This is implemented by the runtime for use by plugins.
 */
public interface ClientConnectionContext {

    /**
     * Return the client's certificate, if a TLS client certificate was presented during TLS handshake.
     * @return the client's certificate, or null if no TLS client certificate was presented during TLS handshake.
     */
    @Nullable X509Certificate clientCertificate();

    /**
     * Determines whether the connection to the client is secured using TLS
     * @return true iff the connection to the client is TLS.
     */
    boolean isClientConnectionTls();

    /**
     * The TLS server certificate that the proxy presented to the client during TLS handshake, if the connection is TLS.
     * @return The proxy's certificate, or null if the transport protocol is not TLS.
     */
    @Nullable X509Certificate proxyServerCertificate();
}
/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.proxy.authentication;

import java.security.cert.X509Certificate;

import edu.umd.cs.findbugs.annotations.Nullable;

/**
 * This interface may be implemented by {@link io.kroxylicious.proxy.filter.Filter}s
 * to learn about server authentication outcomes.
 */
public interface ServerConnectionAware {

    /**
     * Called when the proxy authenticates, or reauthenticates with a server
     * The given serverSubject may not have a principal for the server corresponding
     * to the authentication mechanism used if that authentication mechanism does not provide
     * mutual authentication.
     * @param context The context.
     */
    void onServerConnection(Context context);

    /**
     * The context API for {@link ServerConnectionAware}.
     * This is implemented by the runtime for use by plugins.
     */
    interface Context {

        /**
         * Return the Kafka server's certificate, if the connection with the server is TLS.
         * @return the server's certificate, or null if the connection with the server is not TLS.
         */
        @Nullable
        X509Certificate serverCertificate();

        /**
         * Determines whether the connection to the server is secured using TLS
         * @return true iff the connection to the server is TLS.
         */
        boolean isServerConnectionTls();

        /**
         * The TLS client certificate that the proxy presented to the client during TLS handshake,
         * if the connection with the Kafka server is mTLS.
         * @return The proxy's TLS client certificate, or null if the proxy
         * did not present a client certificate.
         */
        @Nullable X509Certificate proxyClientCertificate();

    }
}

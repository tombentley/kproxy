/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.proxy.authentication;

import java.security.cert.X509Certificate;

import edu.umd.cs.findbugs.annotations.Nullable;

/**
 * <p>This interface may be implemented by {@link io.kroxylicious.proxy.filter.Filter}s
 * to learn about connection details.</p>
 */
public interface ClientConnectionAware {

    /**
     * Notification that the transport is ready for Kafka protocol exchanges.
     * @param context The context.
     */
    void onClientConnection(Context context);

//    /**
//     * Notification of successful TLS authentication of a client.
//     * Called with a non-null {@code clientPrincipal} when a client authenticates, or reauthenticates, with the proxy using a client TLS certificate.
//     * Called with a null {@code clientPrincipal} if:
//     * <ul>
//     *     <li>the connection is not configured to require client TLS authentication and the client presented no TLS certificate,</li>
//     *     <li>if the transport protocol is not TLS</li>
//     * </ul>
//     * @param clientCertificate The client's certificate.
//     * @param context The authentication context.
//     */
//    void onClientTlsAuthentication(X509Certificate clientCertificate,
//                                   ClientTlsContext context);

    /**
     * The context API for {@link ClientConnectionAware}.
     * This is implemented by the runtime for use by plugins.
     */
    interface Context {

        /**
         * Return the client's certificate, if a TLS client certificate was presented during TLS handshake.
         * @return the client's certificate, or null if no TLS client certificate was presented during TLS handshake.
         */
        @Nullable
        X509Certificate clientCertificate();

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
}

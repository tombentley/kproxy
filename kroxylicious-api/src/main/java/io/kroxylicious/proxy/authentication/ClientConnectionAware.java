/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.proxy.authentication;

import io.kroxylicious.proxy.filter.Filter;

/**
 * <p>This interface may be implemented by {@link Filter}s to learn about connection details.</p>
 */
public interface ClientConnectionAware {

    /**
     * Notification that the transport is ready for Kafka protocol exchanges.
     * @param context The context.
     */
    void onClientConnection(ClientConnectionContext context);

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
}

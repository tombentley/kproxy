/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.proxy.authentication;

import javax.security.auth.x500.X500Principal;

import io.kroxylicious.proxy.filter.Filter;

import edu.umd.cs.findbugs.annotations.Nullable;

/**
 * <p>This interface may be implemented by {@link Filter}s to learn about client authentication outcomes.</p>
 *
 * <p>The runtime guarantees that <em>eventually</em> either:
 * <ul>
 * <li>both {@link #onClientTlsAuthentication(X500Principal, ClientAuthenticationContext)}
 * and {@link #onClientSaslAuthentication(SaslPrincipal, ClientAuthenticationContext)}
 * are called, or </li>
 * <li>{@link #onClientAuthenticationFailure(Exception, ClientAuthenticationContext)} is called,
 * possibly after a call to {@link #onClientTlsAuthentication(X500Principal, ClientAuthenticationContext)}</li>
 * </ul>
 * before requests requiring authentication are propagated to the Kafka server.
 * "Requests requiring authentication" means any Kafka API except for {@code ApiVersions},
 * {@code SaslHandshake} or {@code SaslAuthenticate}.
 * </p>
 */
public interface ClientSubjectAware {

    /**
     * Notification of successful TLS authentication of a client.
     * Called with a non-null {@code clientPrincipal} when a client authenticates, or reauthenticates, with the proxy using a client TLS certificate.
     * Called with a null {@code clientPrincipal} if:
     * <ul>
     *     <li>the connection is not configured to require client TLS authentication and the client presented no TLS certificate,</li>
     *     <li>if the transport protocol is not TLS</li>
     * </ul>
     * @param clientPrincipal The principal that has been authenticated, or null if TLS authentication was not required and not performed.
     * @param context The authentication context.
     */
    void onClientTlsAuthentication(@Nullable X500Principal clientPrincipal,
                                   ClientAuthenticationContext context);

    /**
     * Notification of successful SASL authentication of a client.
     * Called with a non-null {@code clientPrincipal} when a client authenticates, or reauthenticates, with the proxy using SASL.
     * Called with a null {@code clientPrincipal} if:
     * <ul>
     *     <li>SASL authentication is not required and the client progressed to making
     *      * a request which would require SASL authentication without sending a {@code SaslAuthenticate} request.</li>
     * </ul>
     * @param clientPrincipal The principal that has been authenticated, or null if SASL authentication was not required and not performed.
     * @param context The authentication context.
     */
    void onClientSaslAuthentication(@Nullable SaslPrincipal clientPrincipal,
                                    ClientAuthenticationContext context);

    /**
     * Notification for failed client authentication outcomes.
     * Called when a client fails authentication, or reauthentication, with the proxy, for any reason.
     * @param exception The cause of the authentication failure.
     * @param context The authentication context.
     */
    default void onClientAuthenticationFailure(Exception exception,
                                               ClientAuthenticationContext context) {
    }
}

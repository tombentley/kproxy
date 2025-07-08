/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.proxy.authentication;

import edu.umd.cs.findbugs.annotations.Nullable;

/**
 * <p>This interface may be implemented by {@link io.kroxylicious.proxy.filter.Filter}s
 * to learn about server SASL authentication outcomes.</p>
 */
public interface ServerSaslAware {

    /**
     * Notification of successful SASL authentication with a server.
     * Called with a non-null {@code clientPrincipal} when a client authenticates, or reauthenticates, with the proxy using SASL.
     * Called with a null {@code clientPrincipal} if:
     * <ul>
     *     <li>SASL authentication is not required and the client progressed to making
     *      a request which would require SASL authentication without sending a {@code SaslAuthenticate} request.</li>
     * </ul>
     * @param context The authentication context.
     */
    void onServerSaslAuthentication(Context context);

//    /**
//     * Called when the proxy fails authentication, or reauthentication, with a server
//     * @param exception The cause of the authentication failure.
//     * @param context The authentication context.
//     */
//    default void onServerAuthenticationFailure(LoginException exception, ServerSaslContext context) {
//    }

    /**
     * The context API for {@link ServerSaslAware}.
     * This is implemented by the runtime for use by plugins.
     */
    interface Context {
        /**
         * The name of the SASL mechanism used.
         * @return The name of the SASL mechanism used.
         */
        String serverSaslMechanismName();

        /**
         * Returns the server's principal if the server has authenticated
         * using a SASL mechanism supporting mutual authentication.
         * @return the server's principal,
         * or null if the server did not provide a principal because the SASL mechanism used
         * does not support mutual authentication.
         */
        @Nullable
        SaslPrincipal serverPrincipal();

        /**
         * A principal representing the identity that the proxy presented to the server during SASL authentication.
         * @return the proxy's principal with the server. This will be null
         * if the proxy has not performed SASL authentication.
         */
        @Nullable SaslPrincipal proxyServerPrincipal();
    }
}

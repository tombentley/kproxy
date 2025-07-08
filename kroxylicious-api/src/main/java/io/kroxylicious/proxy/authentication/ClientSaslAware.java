/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.proxy.authentication;

import edu.umd.cs.findbugs.annotations.Nullable;

/**
 * <p>This interface may be implemented by
 * {@link io.kroxylicious.proxy.filter.Filter}s to learn
 * about client SASL authentication outcomes.</p>
 */
public interface ClientSaslAware {

    /**
     * <p>Notification of a non-failed SASL authentication of a client.
     * A "non-failed SASL authentication" is either a successful SASL authentication
     * or the use of a client that has not performed authentication when authentication
     * is not required (that is, an anonymous client).
     *
     * <p>The runtime guarantees that this method will be called initially
     * before requests requiring authentication
     * are propagated to the Kafka server.
     * "Requests requiring authentication" means any Kafka API except for {@code ApiVersions},
     * {@code SaslHandshake} or {@code SaslAuthenticate}.</p>
     *
     * <p>This method may be called multiple
     * times in the lifetime of a plugin instance, for instance if reauthentication
     * is performed.</p>
     *
     * @param context The authentication context.
     */
    void onClientSaslAuthentication(Context context);

//    /**
//     * Notification for failed client authentication outcomes.
//     * Called when a client fails authentication, or reauthentication, with the proxy, for any reason.
//     * @param exception The cause of the authentication failure.
//     * @param context The authentication context.
//     */
//    default void onClientAuthenticationFailure(Exception exception,
//                                               ClientSaslContext context) {
//    }

    /**
     * The context API for {@link ClientSaslAware}.
     * This is implemented by the runtime for use by plugins.
     */
    interface Context {

        /**
         * The name of the SASL mechanism used.
         * @return The name of the SASL mechanism used.
         */
        String saslMechanismName();

        /**
         * Returns the client's principal if the client has authenticated using SASL.
         * @return the client's principal,
         * or null if the client has not attempted authentication.
         */
        @Nullable
        SaslPrincipal clientPrincipal();

        /**
         * A principal representing the identity that the proxy presented to the client using SASL authentication.
         * @return the proxy's principal with the client. This will be null
         * if the client has not attempted authentication,
         * or if the proxy did not use a principal because the SASL mechanism used
         * does not support mutual authentication.
         */
        @Nullable SaslPrincipal proxyServerPrincipal();
    }
}

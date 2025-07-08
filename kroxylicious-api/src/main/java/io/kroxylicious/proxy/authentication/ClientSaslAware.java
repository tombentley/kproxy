/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.proxy.authentication;

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
    void onClientSaslAuthentication(ClientSaslContext context);

//    /**
//     * Notification for failed client authentication outcomes.
//     * Called when a client fails authentication, or reauthentication, with the proxy, for any reason.
//     * @param exception The cause of the authentication failure.
//     * @param context The authentication context.
//     */
//    default void onClientAuthenticationFailure(Exception exception,
//                                               ClientSaslContext context) {
//    }
}

/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.proxy.authentication;

import javax.security.auth.Subject;
import javax.security.auth.login.LoginException;

import io.kroxylicious.proxy.filter.Filter;

/**
 * This interface may be implemented by {@link Filter}s to learn about client authentication outcomes.
 */
public interface ClientSubjectAware {

    /**
     * Called when a client authenticates, or reauthenticates, with the proxy.
     * @param clientSubject The subject that has been authenticated.
     * @param context The authentication context.
     */
    void onClientAuthentication(Subject clientSubject, ClientAuthenticationContext context);

    /**
     * Called when a client fails authentication, or reauthentication, with the proxy.
     * @param exception The cause of the authentication failure.
     * @param context The authentication context.
     */
    default void onClientAuthenticationFailure(LoginException exception, ClientAuthenticationContext context) {
    }
}

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
 * This interface may be implemented by {@link Filter}s to learn about server authentication outcomes.
 */
public interface ServerSubjectAware  {

    /**
     * Called when the proxy authenticates, or reauthenticates with a server
     * The given serverSubject may not have a principal for the server corresponding
     * to the authentication mechanism used if that authentication mechanism does not provide
     * mutual authentication.
     * @param serverSubject The subject that has been authenticated.
     * @param context The context.
     */
    void onServerAuthentication(Subject serverSubject, ServerAuthenticationContext context);

    /**
     * Called when the proxy fails authentication, or reauthentication, with a server
     * @param exception The cause of the authentication failure.
     * @param context The authentication context.
     */
    default void onServerAuthenticationFailure(LoginException exception, ServerAuthenticationContext context) {
    }
}

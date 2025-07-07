/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.proxy.authentication;

import javax.security.auth.Subject;

public interface ClientAuthenticationContext {

    /**
     * The subject that the proxy presented to the client.
     * This may be null if the authentication mechanism does not support
     * mutual authentication.
     */
    Subject proxySubject();
}
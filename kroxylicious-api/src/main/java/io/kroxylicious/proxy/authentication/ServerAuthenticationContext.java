/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.proxy.authentication;

import javax.security.auth.Subject;

/**
 * The context API for {@link ServerSubjectAware}.
 */
public interface ServerAuthenticationContext {
    /**
     * The subject that the proxy presented to the server.
     */
    Subject proxySubject();
}

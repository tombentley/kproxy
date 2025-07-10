/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.proxy.authentication;

import java.security.Principal;

public interface ClientPrincipalAware<P extends Principal> {
    void onClientAuthentication(P clientPrincipal);
}

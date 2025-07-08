/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.proxy.authentication;

import io.kroxylicious.proxy.filter.Filter;

/**
 * This interface may be implemented by {@link Filter}s to learn about server authentication outcomes.
 */
public interface ServerConnectionAware {

    /**
     * Called when the proxy authenticates, or reauthenticates with a server
     * The given serverSubject may not have a principal for the server corresponding
     * to the authentication mechanism used if that authentication mechanism does not provide
     * mutual authentication.
     * @param context The context.
     */
    void onServerConnection(ServerConnectionContext context);


}

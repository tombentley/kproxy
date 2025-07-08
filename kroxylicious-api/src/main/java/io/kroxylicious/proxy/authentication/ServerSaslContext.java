/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.proxy.authentication;

import edu.umd.cs.findbugs.annotations.Nullable;

/**
 * The context API for {@link ServerSaslAware}.
 * This is implemented by the runtime for use by plugins.
 */
public interface ServerSaslContext {
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

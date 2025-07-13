/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.proxy.authentication;

import java.util.Optional;

/**
 * This is implemented by the runtime for use by plugins.
 */
public interface ServerSaslContext {

    /**
     * The name of the SASL mechanism used.
     * @return The name of the SASL mechanism used.
     */
    String mechanismName();

    /**
     * Returns the principal returned by the server.
     * @return the principal returned by the server,
     * or empty if the SASL mechanism used does not support mutual authentication.
     */
    Optional<SaslPrincipal> serverPrincipal();

    /**
     * A principal representing the identity that the proxy presented to the server using SASL authentication.
     * @return the proxy's principal with the server.
     */
    SaslPrincipal proxyClientPrincipal();
}

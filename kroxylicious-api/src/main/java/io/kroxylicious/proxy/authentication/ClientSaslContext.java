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
public interface ClientSaslContext {

    /**
     * The name of the SASL mechanism used.
     * @return The name of the SASL mechanism used.
     */
    String mechanismName();

    /**
     * Returns the client's principal.
     * @return the client's principal.
     */
    SaslPrincipal clientPrincipal();

    /**
     * A principal representing the identity that the proxy presented to the client using SASL authentication.
     * @return the proxy's principal with the client. This will be null
     * if the proxy did not use a principal because the SASL mechanism used
     * does not support mutual authentication.
     */
    Optional<SaslPrincipal> proxyServerPrincipal();
}

/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.proxy.tls;

import java.security.cert.X509Certificate;
import java.util.Optional;

import io.kroxylicious.proxy.filter.FilterContext;

/**
 * Exposes TLS information about the proxy-to-server connection to plugins, for example using {@link FilterContext#serverTlsContext()}.
 * This is implemented by the runtime for use by plugins.
 */
public interface ServerTlsContext {
    /**
     * @return The TLS server certificate that the proxy presented to the server during TLS handshake,
     * or empty if no TLS client certificate was presented during TLS handshake.
     */
    Optional<X509Certificate> proxyClientCertificate();

    // TODO TLS version
    // TODO Cipher suite
    // client IP address
    //

    /**
     * @return the server's TLS certificate.
     */
    X509Certificate serverCertificate();

}

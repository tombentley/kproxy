/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.proxy.authentication;

import java.security.PrivateKey;
import java.security.cert.Certificate;

/**
 * The context API for {@link ServerTlsClientCertificateSupplier}.
 */
public interface ServerCredentialContext {
    /**
     * Returns the default credentials for this target cluster (e.g. from the proxy configuration file).
     * @return the default credentials.
     */
    TlsCredentials defaultTlsCredentials();

    /**
     * Creates some TLS credentials for the given parameters.
     *
     * The equivalent method on {@code FilterFactoryContext} can be used when the credentials
     * are part of the plugin configuration.
     * @param certificate The TLS certificate
     * @param key The key corresponding to the given {@code certificate}.
     * @param intermediateCertificates Intermediate certificates forming the certificate chain up to (but not including)
     * the TLS certificate trusted by the peer.
     * @return The TLS credentials instance.
     * @see io.kroxylicious.proxy.filter.FilterFactoryContext#tlsCredentials(Certificate, PrivateKey, Certificate[])
     */
    TlsCredentials tlsCredentials(Certificate certificate, PrivateKey key, Certificate[] intermediateCertificates);
}

/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.proxy.authentication;

import java.security.PrivateKey;
import java.security.cert.Certificate;

/**
 * Implemented by a {@link io.kroxylicious.proxy.filter.Filter} that provides
 * the credentials for the TLS connection between the proxy and the Kafka server.
 */
public interface ServerTlsCredentialSupplier {
    /**
     * Return the TlsCredentials for the connection.
     * @param context The context.
     * @return the TlsCredentials for the connection.
     */
    TlsCredentials tlsCredentials(Context context);

    /**
     * The context API for {@link ServerTlsCredentialSupplier}.
     * This is implemented by the runtime for use by plugins.
     */
    interface Context {
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
}

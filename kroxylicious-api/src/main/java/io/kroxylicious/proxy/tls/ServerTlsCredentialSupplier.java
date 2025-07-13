/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.proxy.authentication;

import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.util.Optional;
import java.util.concurrent.CompletionStage;

import io.kroxylicious.proxy.tls.ClientTlsContext;

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
    CompletionStage<TlsCredentials> tlsCredentials(Context context);

    /**
     * The context API for {@link ServerTlsCredentialSupplier}.
     * This is implemented by the runtime for use by plugins.
     */
    interface Context {
        Optional<ClientTlsContext> clientTlsContext();
        Optional<ClientSaslContext> clientSaslContext();

        /**
         * Returns the default credentials for this target cluster (e.g. from the proxy configuration file).
         * Implementations of {@link ServerTlsCredentialSupplier} may use this as a fall-back
         * or default, for example if the apply a certificiate-per-client-principal pattern
         * but are being used with an anonymous principal.
         * @return the default credentials.
         */
        TlsCredentials defaultTlsCredentials();

        /**
         * <p>Factory methods for creating TLS credentials for the given parameters.</p>
         *
         * <p>The equivalent method on {@code FilterFactoryContext} can be used when the credentials
         * are known at plugin configuration time.</p>
         *
         * @param certificate The TLS certificate
         * @param key The key corresponding to the given {@code certificate}.
         * @param intermediateCertificates Intermediate certificates forming the certificate chain up to (but not including)
         * the TLS certificate trusted by the peer.
         * @return The TLS credentials instance.
         * see io.kroxylicious.proxy.filter.FilterFactoryContext#tlsCredentials(Certificate, PrivateKey, Certificate[])
         */
        TlsCredentials tlsCredentials(Certificate certificate,
                                      PrivateKey key,
                                      Certificate[] intermediateCertificates);
    }
}

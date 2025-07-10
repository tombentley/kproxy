/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.proxy.authentication;

import java.net.SocketAddress;
import java.security.Principal;
import java.security.cert.X509Certificate;
import java.util.Optional;
import java.util.concurrent.CompletionStage;

// TODO this would be an interface declared, in and used by,
//   and in-tree SaslTermination or ClientSaslInspection filter
public interface PrincipalBuilder<P extends Principal> {

    /**
     * The context API for {@link PrincipalBuilder}.
     * This is implemented by the runtime for use by plugins.
     */
    interface Context {

        /**
         * @return The <a href="https://en.wikipedia.org/wiki/Server_Name_Indication">SNI</a>
         * hostname which the client used during TLS handshake.
         */
        String sniHostname();

        /**
         * @return The source host of the client, taking into account source host information
         * propagated by intermediate proxies.
         * You can think of this as being like HTTP's {@code X-Forwarded-For} header.
         * @see #srcAddress()
         */
        String clientHost();

        /**
         * @return The source port of the client, taking into account source host information
         * propagated by intermediate proxies.
         */
        int clientPort();

        /**
         * @return The address of the remote TCP peer, which may the ultimate client,
         * but could be an intermediate proxy.
         * @see #clientHost()
         */
        SocketAddress srcAddress();

        /**
         * @return Returns the address of the local connection.
         */
        SocketAddress localAddress();

        /// /////////////////////

        /**
         * Determines whether the connection to the client is secured using TLS
         * @return true iff the connection to the client is TLS.
         */
        Optional<String> tlsVersion();

        Optional<String> tlsCipherSuite();

        /**
         * Return the client's certificate, if a TLS client certificate was presented during TLS handshake.
         * @return the client's certificate, or null if no TLS client certificate was presented during TLS handshake.
         */
        Optional<X509Certificate> clientCertificate();

        /**
         * The TLS server certificate that the proxy presented to the client during TLS handshake, if the connection is TLS.
         * @return The proxy's certificate, or null if the transport protocol is not TLS.
         */
        Optional<X509Certificate> proxyServerCertificate();

        /**
         * The name of the SASL mechanism used.
         * @return The name of the SASL mechanism used.
         */
        Optional<String> saslMechanismName();

        /**
         * Returns the client's principal if the client has authenticated using SASL.
         * @return the client's principal,
         * or empty if the client has not attempted authentication.
         */
        Optional<SaslPrincipal> clientPrincipal();

        /**
         * A principal representing the identity that the proxy presented to the client using SASL authentication.
         * @return the proxy's principal with the client. This will be empty
         * if the client has not attempted authentication,
         * or if the proxy did not use a principal because the SASL mechanism used
         * does not support mutual authentication.
         */
        Optional<SaslPrincipal> proxyServerPrincipal();

        /// ///////////////////

        /**
         * @return The name of the client software, if known via ApiVersions request. Otherwise, null.
         */
        Optional<String> clientSoftwareName();

        /**
         * @return The version of the client software, if known via ApiVersions request. Otherwise, null.
         */
        Optional<String> clientSoftwareVersion();

    }

    CompletionStage<P> build(Context context);
}

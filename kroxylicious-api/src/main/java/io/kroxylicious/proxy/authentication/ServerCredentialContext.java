/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.proxy.authentication;

import java.security.PrivateKey;
import java.security.cert.Certificate;

public interface ServerCredentialContext {
    /** The default key for this target cluster (e.g. from the proxy configuration file). */
    TlsCredentials defaultTlsCredentials();
    /** Factory for TlsCredentials */
    TlsCredentials tlsCredentials(Certificate certificate, PrivateKey key, Certificate[] intermediateCertificates);
}

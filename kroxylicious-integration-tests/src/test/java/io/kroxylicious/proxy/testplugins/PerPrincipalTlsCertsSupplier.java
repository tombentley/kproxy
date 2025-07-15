/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.proxy.testplugins;

import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import io.kroxylicious.proxy.tls.ServerTlsCredentialSupplier;
import io.kroxylicious.proxy.tls.TlsCredentials;

public class PerPrincipalTlsCertsSupplier implements ServerTlsCredentialSupplier {

    private final KeyStore keyStore;

    PerPrincipalTlsCertsSupplier(KeyStore keyStore) {
        this.keyStore = keyStore;
    }

    @Override
    public CompletionStage<TlsCredentials> tlsCredentials(Context context) {
        String principalName = context.clientPrincipal().get().getName(); // TODO define exceptional paths
        try {
            // TODO is it key + chain or key + cert + chain??
            var cert = keyStore.getCertificate(principalName);
            if (cert == null) {
                return CompletableFuture.failedFuture(new RuntimeException("KeyStore did not contain a certificate for " + principalName));
            }

            var certChain = keyStore.getCertificateChain(principalName);
            if (certChain == null) {
                return CompletableFuture.failedFuture(new RuntimeException("KeyStore did not contain a certificate chain for " + principalName));
            }

            var key = keyStore.getKey(principalName, new char[0]);
            if (key instanceof PrivateKey privateKey) {
                var creds = context.tlsCredentials(cert, privateKey, certChain);
                return CompletableFuture.completedFuture(creds);
            }
            else {
                return CompletableFuture.failedFuture(new RuntimeException("KeyStore did not contain a private key for " + principalName));
            }
        }
        catch (GeneralSecurityException e) {
            return CompletableFuture.failedFuture(e);
        }
    }
}

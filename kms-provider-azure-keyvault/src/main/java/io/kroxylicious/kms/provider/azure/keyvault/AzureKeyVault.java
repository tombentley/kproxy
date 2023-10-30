/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.kms.provider.azure.keyvault;

import java.util.concurrent.CompletionStage;

import javax.crypto.SecretKey;

import com.azure.security.keyvault.keys.KeyClient;
import com.azure.security.keyvault.keys.KeyClientBuilder;
import com.azure.security.keyvault.keys.cryptography.CryptographyClient;
import com.azure.security.keyvault.keys.cryptography.CryptographyClientBuilder;

import io.kroxylicious.kms.service.De;
import io.kroxylicious.kms.service.DekPair;
import io.kroxylicious.kms.service.Kms;
import io.kroxylicious.kms.service.Ser;

public class AzureKeyVault
        implements
        Kms<KeyVaultKeyRef, KeyVaultEdek> {
    private final KeyClient client;
    private final CryptographyClient cryptoClient;

    AzureKeyVault(KeyVaultOptions options) {

        this.client = new KeyClientBuilder()
                // .endpoint("")
                // .credential(new DefaultAzureCredentialBuilder().build())
                .buildClient();

        this.cryptoClient = new CryptographyClientBuilder()
                .keyIdentifier("<your-key-id-from-key-vault>")
                // .credential(new DefaultAzureCredentialBuilder().build())
                .buildClient();

    }

    @edu.umd.cs.findbugs.annotations.NonNull
    @Override
    public CompletionStage<KeyVaultEdek> generateDek(@edu.umd.cs.findbugs.annotations.NonNull KeyVaultKeyRef kekRef) {
        return null;
    }

    @edu.umd.cs.findbugs.annotations.NonNull
    @Override
    public CompletionStage<DekPair<KeyVaultEdek>> generateDekPair(@edu.umd.cs.findbugs.annotations.NonNull KeyVaultKeyRef kekRef) {
        return null;
    }

    @edu.umd.cs.findbugs.annotations.NonNull
    @Override
    public CompletionStage<SecretKey> decryptEdek(@edu.umd.cs.findbugs.annotations.NonNull KeyVaultKeyRef kek, @edu.umd.cs.findbugs.annotations.NonNull KeyVaultEdek edek) {
        return null;
    }

    @edu.umd.cs.findbugs.annotations.NonNull
    @Override
    public De<KeyVaultKeyRef> keyRefDeserializer() {
        return null;
    }

    @edu.umd.cs.findbugs.annotations.NonNull
    @Override
    public Ser<KeyVaultEdek> edekSerializer() {
        return null;
    }

    @edu.umd.cs.findbugs.annotations.NonNull
    @Override
    public Ser<KeyVaultKeyRef> keyRefSerializer() {
        return null;
    }

    @edu.umd.cs.findbugs.annotations.NonNull
    @Override
    public De<KeyVaultEdek> edekDeserializer() {
        return null;
    }
}

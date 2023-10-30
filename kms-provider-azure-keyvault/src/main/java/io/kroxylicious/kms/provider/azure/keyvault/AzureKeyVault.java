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

import edu.umd.cs.findbugs.annotations.NonNull;

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

    @NonNull
    @Override
    public CompletionStage<KeyVaultEdek> generateDek(@NonNull KeyVaultKeyRef kekRef) {
        return null;
    }

    @NonNull
    @Override
    public CompletionStage<DekPair<KeyVaultEdek>> generateDekPair(@NonNull KeyVaultKeyRef kekRef) {
        return null;
    }

    @NonNull
    @Override
    public CompletionStage<SecretKey> decryptEdek(@NonNull KeyVaultKeyRef kek,
                                                  @NonNull KeyVaultEdek edek) {
        return null;
    }

    @NonNull
    @Override
    public De<KeyVaultKeyRef> keyIdDeserializer() {
        return null;
    }

    @NonNull
    @Override
    public Ser<KeyVaultEdek> edekSerializer() {
        return null;
    }

    @NonNull
    @Override
    public Ser<KeyVaultKeyRef> keyIdSerializer() {
        return null;
    }

    @NonNull
    @Override
    public De<KeyVaultEdek> edekDeserializer() {
        return null;
    }
}

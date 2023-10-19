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

import io.kroxylicious.kms.service.DekGenerator;
import io.kroxylicious.kms.service.Kms;

public class AzureKeyVault
        implements
            Kms<KeyVaultKeyRef, KeyVaultEdek>,
        DekGenerator<KeyVaultKeyRef, KeyVaultEdek> {
    private final KeyClient client;
    private final CryptographyClient cryptoClient;

    AzureKeyVault(KeyVaultOptions options) {

        this.client = new KeyClientBuilder()
                //.endpoint("")
        //.credential(new DefaultAzureCredentialBuilder().build())
                .buildClient();

        this.cryptoClient = new CryptographyClientBuilder()
                .keyIdentifier("<your-key-id-from-key-vault>")
                //.credential(new DefaultAzureCredentialBuilder().build())
                .buildClient();

    }

    @Override
    public CompletionStage<KeyVaultEdek> generateDek(KeyVaultKeyRef kekRef) {
        return null;
    }

    @Override
    public CompletionStage<SecretKey> decryptEdek(KeyVaultKeyRef kek, KeyVaultEdek edek) {
        return null;
    }
}

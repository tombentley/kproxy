/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.kms.provider.hashicorp.vault;

import java.util.concurrent.CompletionStage;
import javax.crypto.SecretKey;

import io.kroxylicious.kms.service.DekGenerator;
import io.kroxylicious.kms.service.Kms;

public class Vault
        implements
            Kms<VaultKeyRef, VaultEdek>,
            DekGenerator<VaultKeyRef, VaultEdek> {
    Vault(VaultOptions options) {

    }

    @Override
    public CompletionStage<VaultEdek> generateDek(VaultKeyRef kekRef) {
        return null;
    }

    @Override
    public CompletionStage<SecretKey> decryptEdek(VaultKeyRef kek, VaultEdek edek) {
        return null;
    }
}

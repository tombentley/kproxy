/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.kms.provider.hashicorp.vault;

import java.util.concurrent.CompletionStage;

import javax.crypto.SecretKey;

import io.kroxylicious.kms.service.De;
import io.kroxylicious.kms.service.DekPair;
import io.kroxylicious.kms.service.Kms;
import io.kroxylicious.kms.service.Ser;

import edu.umd.cs.findbugs.annotations.NonNull;

public class Vault
        implements
        Kms<VaultKeyRef, VaultEdek> {
    Vault(VaultOptions options) {

    }

    @NonNull
    @Override
    public CompletionStage<VaultEdek> generateDek(@NonNull VaultKeyRef kekRef) {
        return null;
    }

    @NonNull
    @Override
    public CompletionStage<DekPair<VaultEdek>> generateDekPair(@NonNull VaultKeyRef kekRef) {
        return null;
    }

    @NonNull
    @Override
    public CompletionStage<SecretKey> decryptEdek(@NonNull VaultKeyRef kek, @NonNull VaultEdek edek) {
        return null;
    }

    @NonNull
    @Override
    public De<VaultKeyRef> keyIdDeserializer() {
        return null;
    }

    @NonNull
    @Override
    public Ser<VaultEdek> edekSerializer() {
        return null;
    }

    @NonNull
    @Override
    public Ser<VaultKeyRef> keyIdSerializer() {
        return null;
    }

    @NonNull
    @Override
    public De<VaultEdek> edekDeserializer() {
        return null;
    }
}

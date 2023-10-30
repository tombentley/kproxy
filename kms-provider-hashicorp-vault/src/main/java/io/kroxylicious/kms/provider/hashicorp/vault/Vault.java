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

public class Vault
        implements
        Kms<VaultKeyRef, VaultEdek> {
    Vault(VaultOptions options) {

    }

    @edu.umd.cs.findbugs.annotations.NonNull
    @Override
    public CompletionStage<VaultEdek> generateDek(@edu.umd.cs.findbugs.annotations.NonNull VaultKeyRef kekRef) {
        return null;
    }

    @edu.umd.cs.findbugs.annotations.NonNull
    @Override
    public CompletionStage<DekPair<VaultEdek>> generateDekPair(@edu.umd.cs.findbugs.annotations.NonNull VaultKeyRef kekRef) {
        return null;
    }

    @edu.umd.cs.findbugs.annotations.NonNull
    @Override
    public CompletionStage<SecretKey> decryptEdek(@edu.umd.cs.findbugs.annotations.NonNull VaultKeyRef kek, @edu.umd.cs.findbugs.annotations.NonNull VaultEdek edek) {
        return null;
    }

    @edu.umd.cs.findbugs.annotations.NonNull
    @Override
    public De<VaultKeyRef> keyRefDeserializer() {
        return null;
    }

    @edu.umd.cs.findbugs.annotations.NonNull
    @Override
    public Ser<VaultEdek> edekSerializer() {
        return null;
    }

    @edu.umd.cs.findbugs.annotations.NonNull
    @Override
    public Ser<VaultKeyRef> keyRefSerializer() {
        return null;
    }

    @edu.umd.cs.findbugs.annotations.NonNull
    @Override
    public De<VaultEdek> edekDeserializer() {
        return null;
    }
}

/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.kms.provider.ibm.keyprotect;

import java.util.concurrent.CompletionStage;

import javax.crypto.SecretKey;

import io.kroxylicious.kms.service.De;
import io.kroxylicious.kms.service.DekPair;
import io.kroxylicious.kms.service.Kms;
import io.kroxylicious.kms.service.Ser;

import edu.umd.cs.findbugs.annotations.NonNull;

public class IbmKeyProtect implements Kms<IbmKeyRef, IbmEdek> {
    IbmKeyProtect(IbmOptions options) {

    }

    @NonNull
    @Override
    public CompletionStage<IbmEdek> generateDek(@NonNull IbmKeyRef kekRef) {
        return null;
    }

    @NonNull
    @Override
    public CompletionStage<DekPair<IbmEdek>> generateDekPair(@NonNull IbmKeyRef kekRef) {
        return null;
    }

    @NonNull
    @Override
    public CompletionStage<SecretKey> decryptEdek(@NonNull IbmKeyRef kek, @NonNull IbmEdek edek) {
        return null;
    }

    @NonNull
    @Override
    public De<IbmKeyRef> keyIdDeserializer() {
        return null;
    }

    @NonNull
    @Override
    public Ser<IbmEdek> edekSerializer() {
        return null;
    }

    @NonNull
    @Override
    public Ser<IbmKeyRef> keyIdSerializer() {
        return null;
    }

    @NonNull
    @Override
    public De<IbmEdek> edekDeserializer() {
        return null;
    }
}

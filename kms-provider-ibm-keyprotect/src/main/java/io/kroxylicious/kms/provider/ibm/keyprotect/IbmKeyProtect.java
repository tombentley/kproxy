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

public class IbmKeyProtect implements Kms<IbmKeyRef, IbmEdek> {
    IbmKeyProtect(IbmOptions options) {

    }

    @edu.umd.cs.findbugs.annotations.NonNull
    @Override
    public CompletionStage<IbmEdek> generateDek(@edu.umd.cs.findbugs.annotations.NonNull IbmKeyRef kekRef) {
        return null;
    }

    @edu.umd.cs.findbugs.annotations.NonNull
    @Override
    public CompletionStage<DekPair<IbmEdek>> generateDekPair(@edu.umd.cs.findbugs.annotations.NonNull IbmKeyRef kekRef) {
        return null;
    }

    @edu.umd.cs.findbugs.annotations.NonNull
    @Override
    public CompletionStage<SecretKey> decryptEdek(@edu.umd.cs.findbugs.annotations.NonNull IbmKeyRef kek, @edu.umd.cs.findbugs.annotations.NonNull IbmEdek edek) {
        return null;
    }

    @edu.umd.cs.findbugs.annotations.NonNull
    @Override
    public De<IbmKeyRef> keyRefDeserializer() {
        return null;
    }

    @edu.umd.cs.findbugs.annotations.NonNull
    @Override
    public Ser<IbmEdek> edekSerializer() {
        return null;
    }

    @edu.umd.cs.findbugs.annotations.NonNull
    @Override
    public Ser<IbmKeyRef> keyRefSerializer() {
        return null;
    }

    @edu.umd.cs.findbugs.annotations.NonNull
    @Override
    public De<IbmEdek> edekDeserializer() {
        return null;
    }
}

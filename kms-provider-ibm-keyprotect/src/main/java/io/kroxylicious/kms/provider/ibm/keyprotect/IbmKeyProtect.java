/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.kms.provider.ibm.keyprotect;

import java.util.concurrent.CompletionStage;

import javax.crypto.SecretKey;

import io.kroxylicious.kms.service.De;
import io.kroxylicious.kms.service.Kms;

import io.kroxylicious.kms.service.Ser;

public class IbmKeyProtect implements Kms<IbmKeyRef, IbmEdek> {
    IbmKeyProtect(IbmOptions options) {

    }

    @Override
    public CompletionStage<IbmEdek> generateDek(IbmKeyRef kekRef) {
        return null;
    }

    @Override
    public CompletionStage<SecretKey> decryptEdek(IbmKeyRef kek, IbmEdek edek) {
        return null;
    }

    @Override
    public De<IbmKeyRef> keyRefDeserializer() {
        return null;
    }

    @Override
    public Ser<IbmEdek> edekSerializer() {
        return null;
    }

    @Override
    public Ser<IbmKeyRef> keyRefSerializer() {
        return null;
    }

    @Override
    public De<IbmEdek> edekDeserializer() {
        return null;
    }
}

/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.kms.provider.ibm.keyprotect;

import java.util.concurrent.CompletionStage;

import javax.crypto.SecretKey;

import io.kroxylicious.kms.service.DekGenerator;
import io.kroxylicious.kms.service.Kms;

public class IbmKeyProtect implements Kms<IbmKeyRef, IbmEdek>, DekGenerator<IbmKeyRef, IbmEdek> {
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
}

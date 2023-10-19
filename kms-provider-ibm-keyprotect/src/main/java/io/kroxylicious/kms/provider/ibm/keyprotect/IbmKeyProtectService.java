/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.kms.provider.ibm.keyprotect;

import io.kroxylicious.kms.service.KmsService;

import org.apache.kafka.common.serialization.Deserializer;
import org.apache.kafka.common.serialization.Serializer;

public class IbmKeyProtectService implements KmsService<IbmOptions, IbmKeyRef, IbmEdek> {
    public IbmKeyProtect buildKms(IbmOptions options) {
        return new IbmKeyProtect(options);
    }

    @Override
    public Serializer<IbmKeyRef> keyRefSerializer() {
        return null;
    }

    @Override
    public Deserializer<IbmEdek> edekDeserializer() {
        return null;
    }
}

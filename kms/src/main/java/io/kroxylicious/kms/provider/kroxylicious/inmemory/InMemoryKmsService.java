/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.kms.provider.kroxylicious.inmemory;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import javax.crypto.SecretKey;

import io.kroxylicious.kms.service.Kms;
import io.kroxylicious.kms.service.KmsService;

public class InMemoryKmsService implements KmsService<Object, UUID, InMemoryEdek> {
    private final Map<UUID, SecretKey> keys = new HashMap<>();

    @Override
    public Kms<UUID, InMemoryEdek> buildKms(Object options) {
        return new InMemoryKms(keys);
    }

}

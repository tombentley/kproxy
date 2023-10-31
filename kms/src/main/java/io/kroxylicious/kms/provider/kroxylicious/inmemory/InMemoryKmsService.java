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

import io.kroxylicious.kms.service.KmsService;

import edu.umd.cs.findbugs.annotations.NonNull;

public class InMemoryKmsService implements KmsService<InMemoryKmsService.Config, UUID, InMemoryEdek> {
    public static record Config(int numIvBytes, int numAuthBits) {
        public Config {
            if (numIvBytes < 1) {
                throw new IllegalArgumentException();
            }
            if (numAuthBits < 1) {
                throw new IllegalArgumentException();
            }
        }
    }

    private final Map<UUID, SecretKey> keys = new HashMap<>();
    private final Map<String, UUID> aliases = new HashMap<>();

    @NonNull
    @Override
    public InMemoryKms buildKms(Config options) {
        return new InMemoryKms(options.numIvBytes(), options.numAuthBits(), keys, aliases);
    }

}

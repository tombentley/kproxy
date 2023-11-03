/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.kms.provider.kroxylicious.inmemory;

import java.util.Map;
import java.util.ServiceLoader;
import java.util.UUID;

import javax.crypto.SecretKey;

import io.kroxylicious.kms.service.KmsService;
import io.kroxylicious.proxy.plugin.Plugin;

import edu.umd.cs.findbugs.annotations.NonNull;

/**
 * The service interface for {@link InMemoryKms}, to be used only for testing.
 * You can obtain an instance via {@link ServiceLoader} or just use the factory method
 * {@link #newInstance()}.
 * An instance of this class encapsulates the set of keys and aliases, which will be shared between
 * the instances created via {@link #buildKms(Config)}.
 * In that respect the {@link InMemoryKms} behaves like a client of a KMS service.
 */
@Plugin(configType = InMemoryKmsService.Config.class)
public class InMemoryKmsService implements KmsService<InMemoryKmsService.Config, UUID, InMemoryEdek> {

    public static InMemoryKmsService newInstance() {
        return (InMemoryKmsService) ServiceLoader.load(KmsService.class).stream()
                .filter(p -> p.type() == InMemoryKmsService.class)
                .findFirst()
                .get()
                .get();
    }

    public record Config(int numIvBytes, int numAuthBits, Map<UUID, SecretKey> keys, Map<String, UUID> aliases) {
        public Config {
            if (numIvBytes < 1) {
                throw new IllegalArgumentException();
            }
            if (numAuthBits < 1) {
                throw new IllegalArgumentException();
            }
        }

        public Config(int numIvBytes, int numAuthBits) {
            this(numIvBytes, numAuthBits, Map.of(), Map.of());
        }
    }

    @NonNull
    @Override
    public InMemoryKms buildKms(Config options) {
        return new InMemoryKms(options.numIvBytes(), options.numAuthBits(), options.keys(), options.aliases());
    }

}

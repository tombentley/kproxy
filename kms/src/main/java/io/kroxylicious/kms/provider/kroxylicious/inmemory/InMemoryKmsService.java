/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.kms.provider.kroxylicious.inmemory;

import io.kroxylicious.kms.service.DekGenerator;
import io.kroxylicious.kms.service.DekGeneratorService;
import io.kroxylicious.kms.service.Kms;
import io.kroxylicious.kms.service.KmsService;

import org.apache.kafka.common.serialization.Deserializer;
import org.apache.kafka.common.serialization.Serializer;
import org.apache.kafka.common.serialization.UUIDDeserializer;
import org.apache.kafka.common.serialization.UUIDSerializer;

import javax.crypto.SecretKey;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class InMemoryKmsService implements KmsService<Object, UUID, InMemoryEdek>, DekGeneratorService<Void, UUID, InMemoryEdek> {
    private final Map<UUID, SecretKey> keys = new HashMap<>();
    @Override
    public Kms<UUID, InMemoryEdek> buildKms(Object options) {
        return new InMemoryKms(keys);
    }

    @Override
    public Serializer<UUID> keyRefSerializer() {
        return new UUIDSerializer();
    }

    @Override
    public Deserializer<InMemoryEdek> edekDeserializer() {
        return new InMemoryEdekDeserializer();
    }

    @Override
    public DekGenerator<UUID, InMemoryEdek> dekGenerator() {
        return new InMemoryKms(keys);
    }

    @Override
    public Deserializer<UUID> keyRefDeserializer() {
        return new UUIDDeserializer();
    }

    @Override
    public Serializer<InMemoryEdek> edekSerializer() {
        return new InMemoryEdekSerializer();
    }
}

/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.encryption;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.ServiceLoader;
import java.util.concurrent.ExecutionException;

import org.junit.jupiter.api.Test;

import io.kroxylicious.kms.provider.kroxylicious.inmemory.InMemoryKms;
import io.kroxylicious.kms.provider.kroxylicious.inmemory.InMemoryKmsService;
import io.kroxylicious.kms.service.KmsService;

import static org.junit.jupiter.api.Assertions.*;

class DekContextTest {

    private static InMemoryKmsService createServiceInstance() {
        return (InMemoryKmsService) ServiceLoader.load(KmsService.class).stream()
                .filter(p -> p.type() == InMemoryKmsService.class)
                .findFirst()
                .get()
                .get();
    }

    @Test
    void foo() throws ExecutionException, InterruptedException {
        InMemoryKms kms = createServiceInstance().buildKms(new InMemoryKmsService.Config(12, 128));
        var kek = kms.generateKey();
        var pair = kms.generateDekPair(kek).get();
        var context = new DekContext<>(kek, null, new AesGcmEncryptor(new AesGcmIvGenerator(new SecureRandom()), pair.dek()));

        ByteBuffer bb = ByteBuffer.wrap("hello, world".getBytes(StandardCharsets.UTF_8));
        int size = context.encodedSize(bb.capacity());
        assertEquals(1, size);
        var output = ByteBuffer.allocate(size);
        context.encode(bb, output);
        assertFalse(output.hasRemaining());
        output.flip();

        var roundTrip = ByteBuffer.allocate(bb.capacity());
        context.decode(output, roundTrip);
        roundTrip.flip();
        assertEquals("hello, world!", new String(roundTrip.array(), StandardCharsets.UTF_8));
    }
}

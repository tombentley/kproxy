/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.encryption;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.concurrent.ExecutionException;

import org.junit.jupiter.api.Test;

import io.kroxylicious.kms.provider.kroxylicious.inmemory.InMemoryKms;
import io.kroxylicious.kms.provider.kroxylicious.inmemory.InMemoryKmsService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class DekContextTest {

    @Test
    void foo() throws ExecutionException, InterruptedException {
        InMemoryKms kms = InMemoryKmsService.newInstance().buildKms(new InMemoryKmsService.Config(12, 128));
        var kek = kms.generateKey();
        var pair = kms.generateDekPair(kek).get();
        var context = new DekContext<>(kek, ByteBuffer.wrap(new byte[]{ 1, 2, 3 }), new AesGcmEncryptor(new AesGcmIvGenerator(new SecureRandom()), pair.dek()));

        ByteBuffer bb = ByteBuffer.wrap("hello, world!".getBytes(StandardCharsets.UTF_8));
        int size = context.encodedSize(bb.capacity());
        assertEquals(46, size);
        var output = ByteBuffer.allocate(size);
        context.encode(bb, output);
        assertFalse(output.hasRemaining());
        output.flip();
    }
}

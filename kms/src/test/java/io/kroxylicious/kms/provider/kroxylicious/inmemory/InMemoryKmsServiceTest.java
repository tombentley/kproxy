/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.kms.provider.kroxylicious.inmemory;

import io.kroxylicious.kms.service.KmsService;

import io.kroxylicious.kms.service.Ser;

import io.kroxylicious.kms.service.UnknownKeyException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;
import java.util.ServiceLoader;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static org.junit.jupiter.api.Assertions.*;

class InMemoryKmsServiceTest {

    InMemoryKmsService service;

    @BeforeEach
    public void before() {
        service = createServiceInstance();
    }

    private static InMemoryKmsService createServiceInstance() {
        return (InMemoryKmsService) ServiceLoader.load(KmsService.class).stream()
                .filter(p -> p.type() == InMemoryKmsService.class)
                .findFirst()
                .get()
                .get();
    }

    @Test
    void shouldRejectOutOfBoundIvBytes() {
        // given
        assertThrows(IllegalArgumentException.class, () -> new InMemoryKmsService.Config(0, 128));
    }

    @Test
    void shouldRejectOutOfBoundAuthBits() {
        assertThrows(IllegalArgumentException.class, () -> new InMemoryKmsService.Config(12, 0));
    }

    @Test
    void shouldSerializeAndDeserialiseKeks() {
        // given
        var kms = service.buildKms(new InMemoryKmsService.Config(12, 128));
        var kek = kms.generateKey();
        assertNotNull(kek);

        // when
        Ser<UUID> kekSer = kms.keyRefSerializer();
        var buffer = ByteBuffer.allocate(kekSer.sizeOf(kek));
        kekSer.serialize(kek, buffer);
        assertFalse(buffer.hasRemaining());
        buffer.flip();
        var kekDe = kms.keyRefDeserializer();
        var loadedKek = kekDe.deserialize(buffer);

        //then
        assertEquals(kek, loadedKek, "Expect the deserialized kek to be equal to the original kek");
    }

    @Test
    void shouldGenerateDeks() {
        // given
        var kms1 = service.buildKms(new InMemoryKmsService.Config(12, 128));
        var kms2 = createServiceInstance().buildKms(new InMemoryKmsService.Config(12, 128));
        var key1 = kms1.generateKey();
        assertNotNull(key1);
        var key2 = kms2.generateKey();
        assertNotNull(key2);

        // when
        CompletableFuture<InMemoryEdek> gen1 = kms1.generateDek(key2);
        CompletableFuture<InMemoryEdek> gen2 = kms2.generateDek(key1);

        // then
        assertNotNull(kms1.generateDek(key1), "Expect kms to be able to generate deks for its own key");
        assertNotNull(kms2.generateDek(key2), "Expect kms to be able to generate deks for its own key");

        var e1 = assertThrows(ExecutionException.class, gen1::get,
                "Expect kms to not generate dek for another kms's key");
        assertInstanceOf(UnknownKeyException.class, e1.getCause());

        var e2 = assertThrows(ExecutionException.class, gen2::get,
                "Expect kms to not generate dek for another kms's key");
        assertInstanceOf(UnknownKeyException.class, e2.getCause());
    }


    @Test
    void shouldDecryptDeks() throws ExecutionException, InterruptedException {
        // given
        var kms = service.buildKms(new InMemoryKmsService.Config(12, 128));
        var kek = kms.generateKey();
        assertNotNull(kek);
        var pair = kms.generateDekPair(kek).get();
        assertNotNull(pair);
        assertNotNull(pair.edek());
        assertNotNull(pair.dek());

        // when
        var decryptedDek = kms.decryptEdek(kek, pair.edek()).get();

        // then
        assertEquals(pair.dek(), decryptedDek, "Expect the decrypted DEK to equal the originally generated DEK");
    }

    @Test
    void shouldSerializeAndDeserializeEdeks() throws ExecutionException, InterruptedException {
        var kms = service.buildKms(new InMemoryKmsService.Config(12, 128));
        var kek = kms.generateKey();

        var edek = kms.generateDek(kek).get();

        var ser = kms.edekSerializer();
        var buffer = ByteBuffer.allocate(ser.sizeOf(edek));
        ser.serialize(edek, buffer);
        assertFalse(buffer.hasRemaining());
        buffer.flip();

        var de = kms.edekDeserializer();
        var deserialized = de.deserialize(buffer);

        assertEquals(edek, deserialized)
;

    }

}
/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.kms.provider.kroxylicious.inmemory;

import java.nio.ByteBuffer;
import java.security.GeneralSecurityException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.Supplier;

import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

import io.kroxylicious.kms.service.De;
import io.kroxylicious.kms.service.Kms;
import io.kroxylicious.kms.service.KmsException;
import io.kroxylicious.kms.service.Ser;
import io.kroxylicious.kms.service.UnknownKeyException;

import org.apache.kafka.common.serialization.UUIDDeserializer;
import org.apache.kafka.common.serialization.UUIDSerializer;

public class InMemoryKms implements
        Kms<UUID, InMemoryEdek> {

    private static final String WRAP_ALGO = "AES_256/GCM/NoPadding";
    public static final String KEY_ALGO = "AES";
    private final Map<UUID, SecretKey> keys;
    private final KeyGenerator aes;
    private final int numIvBytes;
    private final int numAuthBits;
    private SecureRandom secureRandom;

    public InMemoryKms(Map<UUID, SecretKey> keys) {
        this.keys = keys;
        this.secureRandom = new SecureRandom();
        this.numIvBytes = 4;
        this.numAuthBits = 16;
        try {
            this.aes = KeyGenerator.getInstance(KEY_ALGO);
        }
        catch (NoSuchAlgorithmException e) {
            // This should be impossible, because JCA guarantees that AES is available
            throw new KmsException(e);
        }
    }

    /**
     * Generates a KEK
     * @return The id of the KEK
     */
    public UUID generateKey() {
        var key = aes.generateKey();
        var ref = UUID.randomUUID();
        keys.put(ref, key);
        return ref;
    }

    @Override
    public CompletionStage<InMemoryEdek> generateDek(UUID kekRef) {
        try {
            return CompletableFuture.completedFuture(wrap(kekRef, () -> this.aes.generateKey()));
        } catch (KmsException e) {
            return CompletableFuture.failedFuture(e);
        }
    }

    private InMemoryEdek wrap(UUID kekRef, Supplier<SecretKey> generator) {
        SecretKey kek = lookupKey(kekRef);
        Cipher aes = aesGcm();
        GCMParameterSpec spec = aesGcmSpec(kek, aes);
        var dek = generator.get();
        byte[] edek;
        try {
            edek = aes.wrap(dek);
        }
        catch (IllegalBlockSizeException | InvalidKeyException e) {
            throw new KmsException(e);
        }
        return new InMemoryEdek(spec.getTLen(), spec.getIV(), edek);
    }


    @Override
    public CompletionStage<DekPair<InMemoryEdek>> generateDekPair(UUID kekRef) {
        try {
            var dek = this.aes.generateKey();
            var edek = wrap(kekRef, () -> dek);
            return CompletableFuture.completedFuture(new DekPair<>(edek, dek));
        } catch (KmsException e) {
            return CompletableFuture.failedFuture(e);
        }
    }


    private GCMParameterSpec aesGcmSpec(SecretKey kek, Cipher aes) {
        byte[] iv = new byte[numIvBytes];
        secureRandom.nextBytes(iv);
        var spec = new GCMParameterSpec(numAuthBits, iv);
        try {
            aes.init(Cipher.WRAP_MODE, kek, spec);
        }
        catch (GeneralSecurityException e) {
            throw new KmsException(e);
        }
        return spec;
    }

    private SecretKey lookupKey(UUID kekRef) {
        SecretKey kek = this.keys.get(kekRef);
        if (kek == null) {
            throw new UnknownKeyException();
        }
        return kek;
    }

    @Override
    public CompletionStage<SecretKey> decryptEdek(UUID kekRef, InMemoryEdek edek) {
        try {
            var kek = lookupKey(kekRef);
            Cipher aes = aesGcm();
            var spec = new GCMParameterSpec(edek.numAuthBits(), edek.iv());
            try {
                aes.init(Cipher.UNWRAP_MODE, kek, spec);
            }
            catch (InvalidKeyException e) {
                throw new KmsException(e);
            }
            catch (GeneralSecurityException e) {
                throw new KmsException(e);
            }
            SecretKey key;
            try {
                key = (SecretKey) aes.unwrap(edek.edek(), KEY_ALGO, Cipher.SECRET_KEY);
            }
            catch (GeneralSecurityException e) {
                throw new KmsException(e);
            }
            return CompletableFuture.completedFuture(key);
        }
        catch (KmsException e) {
            return CompletableFuture.failedFuture(e);
        }
    }

    private static Cipher aesGcm() {
        Cipher aes = null;
        try {
            aes = Cipher.getInstance(WRAP_ALGO);
        }
        catch (GeneralSecurityException e) {
            throw new KmsException(e);
        }
        return aes;
    }

    static class UUIDSerde implements Ser<UUID>, De<UUID> {

        @Override
        public UUID deserialize(ByteBuffer buffer) {
            var msb = buffer.getLong();
            var lsb = buffer.getLong();
            return new UUID(msb, lsb);
        }

        @Override
        public int sizeOf(UUID uuid) {
            return 16;
        }

        @Override
        public void serialize(UUID uuid, ByteBuffer buffer) {
            buffer.putLong(uuid.getMostSignificantBits());
            buffer.putLong(uuid.getLeastSignificantBits());
        }
    }

    @Override
    public Ser<UUID> keyRefSerializer() {
        return new UUIDSerde();
    }

    class InMemoryEdekSerde implements Ser<InMemoryEdek>, De<InMemoryEdek> {

        @Override
        public InMemoryEdek deserialize(ByteBuffer buffer) {
            var numAuthBits = buffer.getShort();
            var ivLength = buffer.getShort();
            var iv = new byte[ivLength];
            buffer.get(iv);
            int edekLength = buffer.limit() - buffer.position();
            var edek = new byte[edekLength];
            buffer.get(edek);
            return new InMemoryEdek(numAuthBits, iv, edek);
        }

        @Override
        public int sizeOf(InMemoryEdek inMemoryEdek) {
            return Short.BYTES + Short.BYTES + inMemoryEdek.iv().length + inMemoryEdek.edek().length;
        }

        @Override
        public void serialize(InMemoryEdek inMemoryEdek, ByteBuffer buffer) {
            buffer.putShort((short) inMemoryEdek.numAuthBits());
            buffer.putShort((short) inMemoryEdek.iv().length);
            buffer.put(inMemoryEdek.iv());
            buffer.put(inMemoryEdek.edek());
        }
    }

    @Override
    public De<InMemoryEdek> edekDeserializer() {
        return new InMemoryEdekSerde();
    }

    @Override
    public De<UUID> keyRefDeserializer() {
        return new UUIDSerde();
    }

    @Override
    public Ser<InMemoryEdek> edekSerializer() {
        return new InMemoryEdekSerde();
    }
}

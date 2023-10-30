/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.kms.provider.kroxylicious.inmemory;

import java.security.GeneralSecurityException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

import edu.umd.cs.findbugs.annotations.NonNull;

import io.kroxylicious.kms.service.De;
import io.kroxylicious.kms.service.DekPair;
import io.kroxylicious.kms.service.Kms;
import io.kroxylicious.kms.service.KmsException;
import io.kroxylicious.kms.service.Ser;
import io.kroxylicious.kms.service.UnknownKeyException;

public class InMemoryKms implements
        Kms<UUID, InMemoryEdek> {

    private static final String WRAP_ALGO = "AES_256/GCM/NoPadding";
    public static final String KEY_ALGO = "AES";
    private final Map<UUID, SecretKey> keys;
    private final KeyGenerator aes;
    private final int numIvBytes;
    private final int numAuthBits;
    private SecureRandom secureRandom;

    public InMemoryKms(int numIvBytes, int numAuthBits, Map<UUID, SecretKey> keys) {
        this.keys = keys;
        this.secureRandom = new SecureRandom();
        this.numIvBytes = numIvBytes;
        this.numAuthBits = numAuthBits;
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

    @NonNull
    @Override
    public CompletableFuture<InMemoryEdek> generateDek(@NonNull UUID kekRef) {
        try {
            return CompletableFuture.completedFuture(wrap(kekRef, this.aes::generateKey));
        }
        catch (KmsException e) {
            return CompletableFuture.failedFuture(e);
        }
    }

    private InMemoryEdek wrap(UUID kekRef, Supplier<SecretKey> generator) {
        SecretKey kek = lookupKey(kekRef);
        Cipher aesCipher = aesGcm();
        GCMParameterSpec spec = aesGcmSpec(kek, aesCipher);
        var dek = generator.get();
        byte[] edek;
        try {
            edek = aesCipher.wrap(dek);
        }
        catch (IllegalBlockSizeException | InvalidKeyException e) {
            throw new KmsException(e);
        }
        return new InMemoryEdek(spec.getTLen(), spec.getIV(), edek);
    }

    @NonNull
    @Override
    public CompletableFuture<DekPair<InMemoryEdek>> generateDekPair(@NonNull UUID kekRef) {
        try {
            var dek = this.aes.generateKey();
            var edek = wrap(kekRef, () -> dek);
            return CompletableFuture.completedFuture(new DekPair<>(edek, dek));
        }
        catch (KmsException e) {
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

    @NonNull
    @Override
    public CompletableFuture<SecretKey> decryptEdek(@NonNull UUID kekRef, @NonNull InMemoryEdek edek) {
        try {
            var kek = lookupKey(kekRef);
            Cipher aesCipher = aesGcm();
            var spec = new GCMParameterSpec(edek.numAuthBits(), edek.iv());
            try {
                aesCipher.init(Cipher.UNWRAP_MODE, kek, spec);
            }
            catch (GeneralSecurityException e) {
                throw new KmsException("Error initializing cipher", e);
            }
            SecretKey key;
            try {
                key = (SecretKey) aesCipher.unwrap(edek.edek(), KEY_ALGO, Cipher.SECRET_KEY);
            }
            catch (GeneralSecurityException e) {
                throw new KmsException("Error unwrapping DEK", e);
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

    @NonNull
    @Override
    public Ser<UUID> keyRefSerializer() {
        return new UUIDSerde();
    }

    @NonNull
    @Override
    public De<InMemoryEdek> edekDeserializer() {
        return new InMemoryEdekSerde();
    }

    @NonNull
    @Override
    public De<UUID> keyRefDeserializer() {
        return new UUIDSerde();
    }

    @NonNull
    @Override
    public Ser<InMemoryEdek> edekSerializer() {
        return new InMemoryEdekSerde();
    }
}

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
import java.util.concurrent.CompletionStage;

import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

import io.kroxylicious.kms.service.DekGenerator;
import io.kroxylicious.kms.service.Kms;
import io.kroxylicious.kms.service.KmsException;
import io.kroxylicious.kms.service.UnknownKeyException;

public class InMemoryKms implements
        Kms<UUID, InMemoryEdek>,
        DekGenerator<UUID, InMemoryEdek> {

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
            SecretKey kek = lookupKey(kekRef);
            var dek = this.aes.generateKey();
            Cipher aes = aesGcm();
            GCMParameterSpec spec = aesGcmSpec(kek, aes);
            byte[] edek;
            try {
                edek = aes.wrap(dek);
            }
            catch (IllegalBlockSizeException e) {
                throw new KmsException(e);
            }
            catch (InvalidKeyException e) {
                throw new KmsException(e);
            }
            return CompletableFuture.completedFuture(new InMemoryEdek(spec.getTLen(), spec.getIV(), edek));
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

}

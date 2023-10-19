/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.encryption;

import java.nio.ByteBuffer;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

public class Encryptor {

    private final SecretKey key;
    private final Cipher cipher;
    private final SecureRandom rng;
    private final int numTagBits;
    private final byte[] iv;

    Encryptor(int numIvBytes, int numTagBits, SecureRandom rng, SecretKey key) {
        this.iv = new byte[numIvBytes];
        this.numTagBits = numTagBits;
        this.rng = rng;
        this.key = key;
        try {
            this.cipher = Cipher.getInstance("AES");
        }
        catch (GeneralSecurityException e) {
            throw new EncryptionException(e);
        }
    }

    public int outputSize(int plaintextSize) {
        return 1 + // version
                iv.length + // size of iv
                this.cipher.getOutputSize(plaintextSize);
    }

    /**
     * Encrypt the given ciphertext, outputting the ciphertext and any necessary extra data to the given {@code ciphertext}.
     * @param plaintext
     * @param ciphertext
     */
    public void encrypt(ByteBuffer plaintext, ByteBuffer ciphertext) {
        rng.nextBytes(iv);
        init(Cipher.ENCRYPT_MODE);
        try {
            ciphertext.put((byte) 0);
            ciphertext.put(iv);
            this.cipher.doFinal(plaintext, ciphertext);
        }
        catch (GeneralSecurityException e) {
            throw new EncryptionException(e);
        }
    }

    private void init(int encryptMode) {
        var spec = new GCMParameterSpec(numTagBits, iv);
        try {
            this.cipher.init(encryptMode, key, spec);
        }
        catch (GeneralSecurityException e) {
            throw new EncryptionException(e);
        }
    }

    public void decrypt(ByteBuffer ciphertext, ByteBuffer plaintext) {
        var version = ciphertext.get();
        if (version == 0) {
            ciphertext.get(iv);
            init(Cipher.DECRYPT_MODE);
            try {
                this.cipher.doFinal(ciphertext, plaintext);
            }
            catch (GeneralSecurityException e) {
                throw new EncryptionException(e);
            }
        } else {
            throw new EncryptionException("Unknown version " + version);
        }
    }
}

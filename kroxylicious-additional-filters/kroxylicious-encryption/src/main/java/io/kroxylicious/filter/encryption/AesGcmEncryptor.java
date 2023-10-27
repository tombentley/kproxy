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

public class AesGcmEncryptor {

    private final SecretKey key;
    private final Cipher cipher;
    private final int numTagBits;
    private final byte[] iv;
    private final AesGcmIvGenerator ivGenerator;

    AesGcmEncryptor(AesGcmIvGenerator ivGenerator, SecretKey key) {
        // NIST SP.800-38D recommends 96 bit for recommendation about the iv length and generation
        this.iv = new byte[ivGenerator.sizeBytes()];
        this.ivGenerator = ivGenerator;
        this.numTagBits = 128;
        this.key = key;
        try {
            this.cipher = Cipher.getInstance("AES_256/GCM/NoPadding");
        }
        catch (GeneralSecurityException e) {
            throw new EncryptionException(e);
        }
    }

    public int outputSize(int plaintextSize) {
        return Byte.BYTES + // version
                Byte.BYTES + // iv length
                iv.length + // size of iv
                this.cipher.getOutputSize(plaintextSize);
    }


    /**
     * Encrypt the given plaintext, writing the ciphertext and any necessary extra data to the given {@code output}.
     * @param plaintext The plaintext to encrypt
     * @param output the output buffer
     */
    public void encrypt(ByteBuffer plaintext, ByteBuffer output) {
        ivGenerator.generateIv(iv);
        init(Cipher.ENCRYPT_MODE);
        try {
            output.put((byte) 0); // version
            output.put((byte) iv.length); // iv length
            output.put(iv); // the iv
            this.cipher.doFinal(plaintext, output);
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

    public void decrypt(ByteBuffer input, ByteBuffer plaintext) {
        var version = input.get();
        if (version == 0) {
            int ivLength = input.get();
            if (ivLength != iv.length) {
                throw new EncryptionException("Unexpected IV length");
            }
            input.get(iv, 0, iv.length);
            init(Cipher.DECRYPT_MODE);
            try {
                this.cipher.doFinal(input, plaintext);
            }
            catch (GeneralSecurityException e) {
                throw new EncryptionException(e);
            }
        }
        else {
            throw new EncryptionException("Unknown version " + version);
        }
    }
}

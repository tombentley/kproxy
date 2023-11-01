/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.encryption;

import java.nio.ByteBuffer;
import java.security.GeneralSecurityException;

import javax.annotation.concurrent.NotThreadSafe;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

@NotThreadSafe
class AesGcmEncryptor {

    private final SecretKey key;
    private final Cipher cipher;
    private final int numAuthBits;
    private final byte[] iv;
    private final AesGcmIvGenerator ivGenerator;

    AesGcmEncryptor(AesGcmIvGenerator ivGenerator, SecretKey key) {
        // NIST SP.800-38D recommends 96 bit for recommendation about the iv length and generation
        this.iv = new byte[ivGenerator.sizeBytes()];
        this.ivGenerator = ivGenerator;
        this.numAuthBits = 128;
        this.key = key;
        try {
            this.cipher = Cipher.getInstance("AES_256/GCM/NoPadding");
        }
        catch (GeneralSecurityException e) {
            throw new EncryptionException(e);
        }
    }

    public int outputSize(int plaintextSize) {
        init(Cipher.ENCRYPT_MODE);
        return Byte.BYTES // version
                + Byte.BYTES // iv length
                + ivGenerator.sizeBytes() // iv
                + this.cipher.getOutputSize(plaintextSize);
    }

    /**
     * Encrypt the given plaintext, writing the ciphertext and any necessary extra data to the given {@code output}.
     * @param plaintext The plaintext to encrypt.
     * @param output The output buffer.
     */
    public void encrypt(ByteBuffer plaintext, ByteBuffer output) {
        byte version = 0;
        output.put(version);
        output.put((byte) iv.length);
        ivGenerator.generateIv(iv);
        init(Cipher.ENCRYPT_MODE);
        try {
            output.put(iv); // the iv
            this.cipher.doFinal(plaintext, output);
        }
        catch (GeneralSecurityException e) {
            throw new EncryptionException(e);
        }

    }

    private void init(int encryptMode) {
        var spec = new GCMParameterSpec(numAuthBits, iv);
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

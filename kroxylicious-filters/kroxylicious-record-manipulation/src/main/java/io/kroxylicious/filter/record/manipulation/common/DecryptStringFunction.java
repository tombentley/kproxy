/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.record.manipulation.common;

import java.nio.charset.StandardCharsets;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.function.Function;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * Decrypts a Base64-encoded ciphertext produced by {@link EncryptStringFunction}, using a raw key supplied by the caller.
 */
public class DecryptStringFunction implements Function<String, String> {

    private static final int IV_LENGTH = 12;

    private final byte[] key;
    private final Cipher cipher;

    /**
     * Creates an instance.
     * @param key the raw key used for the AES/GCM operation
     */
    public DecryptStringFunction(byte[] key) {
        this.key = key;
        try {
            cipher = Cipher.getInstance("AES/GCM/NoPadding");
        }
        catch (NoSuchAlgorithmException | NoSuchPaddingException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public String apply(String value) {
        try {
            byte[] iv = new byte[IV_LENGTH];
            byte[] ciphertextAndIv = Base64.getDecoder().decode(value);
            System.arraycopy(ciphertextAndIv, ciphertextAndIv.length - IV_LENGTH, iv, 0, IV_LENGTH);

            cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(key, "AES"), new GCMParameterSpec(96, iv));
            return new String(cipher.doFinal(ciphertextAndIv, 0, ciphertextAndIv.length - iv.length), StandardCharsets.UTF_8);
        }
        catch (IllegalBlockSizeException | BadPaddingException | InvalidKeyException | InvalidAlgorithmParameterException e) {
            throw new RuntimeException(e);
        }
    }
}

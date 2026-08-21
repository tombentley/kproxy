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

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * Decrypts a Base64-encoded ciphertext produced by {@link EncryptStringFunction}, using a raw key drawn
 * from the invocation's {@link Context}. A fresh {@link Cipher} is created per invocation - the key isn't
 * known until then, and a shared, cached instance would not be safe to reuse across concurrent invocations
 * regardless.
 */
public class DecryptStringFunction implements StringOp {

    private static final int IV_LENGTH = 12;

    /**
     * Creates an instance.
     */
    public DecryptStringFunction() {
    }

    @Override
    public String apply(String value, Context context) {
        try {
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            byte[] iv = new byte[IV_LENGTH];
            byte[] ciphertextAndIv = Base64.getDecoder().decode(value);
            System.arraycopy(ciphertextAndIv, ciphertextAndIv.length - IV_LENGTH, iv, 0, IV_LENGTH);

            cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(context.key(), "AES"), new GCMParameterSpec(96, iv));
            return new String(cipher.doFinal(ciphertextAndIv, 0, ciphertextAndIv.length - iv.length), StandardCharsets.UTF_8);
        }
        catch (NoSuchAlgorithmException | NoSuchPaddingException | IllegalBlockSizeException | BadPaddingException | InvalidKeyException
                | InvalidAlgorithmParameterException e) {
            throw new RuntimeException(e);
        }
    }
}

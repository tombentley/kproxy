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
import java.security.SecureRandom;
import java.util.Base64;
import java.util.function.Function;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.Mac;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.ShortBufferException;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public class Strings {

    private final byte[] key;
    Mac mac;
    Cipher cipher;

    public Strings(byte[] key) {
        this.key = key;
        try {
            mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(key, "HmacSHA256"));
            cipher = Cipher.getInstance("AES/GCM/NoPadding");
        }
        catch (NoSuchAlgorithmException | InvalidKeyException | NoSuchPaddingException e) {
            throw new RuntimeException(e);
        }
    }

    public Function<String, String> hmac() {
        return value -> Base64.getEncoder().encodeToString(mac.doFinal(value.getBytes(StandardCharsets.UTF_8)));
    }

    private static final int IV_LENGTH = 12;

    public Function<String, String> encrypt() {
        return value -> {
            try {
                byte[] iv = new byte[IV_LENGTH];
                new SecureRandom().nextBytes(iv);
                // System.out.println("encrypt: " + Arrays.toString(iv));
                cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(key, "AES"), new GCMParameterSpec(96, iv));
                // TODO encode the iv
                byte[] plaintext = value.getBytes(StandardCharsets.UTF_8);
                int ciphertextSize = cipher.getOutputSize(plaintext.length);
                byte[] output = new byte[ciphertextSize + IV_LENGTH];
                int i = cipher.doFinal(plaintext, 0, plaintext.length, output);
                if (i != ciphertextSize) {
                    throw new RuntimeException("Invalid");
                }
                // System.out.println("encrypt: " + Arrays.toString(output));
                System.arraycopy(iv, 0, output, ciphertextSize, IV_LENGTH);
                // System.out.println("encrypt: " + Arrays.toString(output));
                return Base64.getEncoder().encodeToString(output);
            }
            catch (IllegalBlockSizeException | BadPaddingException | InvalidKeyException | InvalidAlgorithmParameterException | ShortBufferException e) {
                throw new RuntimeException(e);
            }
        };
    }

    public Function<String, String> decrypt() {
        return value -> {
            try {
                // TODO read the IV
                byte[] iv = new byte[IV_LENGTH];
                byte[] ciphertextAndIv = Base64.getDecoder().decode(value);
                // System.out.println("decrypt: " + Arrays.toString(ciphertextAndIv));
                System.arraycopy(ciphertextAndIv, ciphertextAndIv.length - IV_LENGTH, iv, 0, IV_LENGTH);
                // System.out.println("decrypt: " + Arrays.toString(iv));

                cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(key, "AES"), new GCMParameterSpec(96, iv));
                return new String(cipher.doFinal(ciphertextAndIv, 0, ciphertextAndIv.length - iv.length), StandardCharsets.UTF_8);
            }
            catch (IllegalBlockSizeException | BadPaddingException | InvalidKeyException | InvalidAlgorithmParameterException e) {
                throw new RuntimeException(e);
            }
        };
    }

    // TODO replaceAll and replaceFirst, trim, split
    // TODO date parsing, and support for "formats"

}

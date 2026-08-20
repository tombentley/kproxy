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
import java.util.Random;
import java.util.function.Function;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.Mac;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.ShortBufferException;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * HMAC and symmetric encrypt/decrypt operations on strings, using a raw key supplied by the caller.
 */
public class Strings {

    private final byte[] key;
    private final Random random;
    Mac mac;
    Cipher cipher;

    /**
     * Creates an instance.
     * @param key the raw key used for both HMAC and AES/GCM operations
     * @param random the source of randomness used to generate AES/GCM initialization vectors
     */
    public Strings(byte[] key, Random random) {
        this.key = key;
        this.random = random;
        try {
            mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(key, "HmacSHA256"));
            cipher = Cipher.getInstance("AES/GCM/NoPadding");
        }
        catch (NoSuchAlgorithmException | InvalidKeyException | NoSuchPaddingException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Computes an HMAC.
     * @return a function computing the Base64-encoded HMAC-SHA256 of its input
     */
    public Function<String, String> hmac() {
        return value -> Base64.getEncoder().encodeToString(mac.doFinal(value.getBytes(StandardCharsets.UTF_8)));
    }

    private static final int IV_LENGTH = 12;

    /**
     * Encrypts a value.
     * @return a function encrypting its input with AES/GCM, returning a Base64-encoded ciphertext with the IV appended
     */
    @SuppressFBWarnings(value = "PREDICTABLE_RANDOM", justification = "The PRNG is deliberately injected rather than SecureRandom, "
            + "so that masking can eventually be made to have repeatable-read semantics (e.g. seeded from topic/partition/offset)")
    public Function<String, String> encrypt() {
        return value -> {
            try {
                byte[] iv = new byte[IV_LENGTH];
                random.nextBytes(iv);
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

    /**
     * Decrypts a value.
     * @return a function decrypting a Base64-encoded ciphertext produced by {@link #encrypt()}
     */
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

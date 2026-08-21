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
import javax.crypto.NoSuchPaddingException;
import javax.crypto.ShortBufferException;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Encrypts a string with AES/GCM, using a raw key supplied by the caller, returning a
 * Base64-encoded ciphertext with the IV appended.
 */
public class EncryptStringFunction implements Function<String, String> {

    private static final int IV_LENGTH = 12;

    private final byte[] key;
    private final Random random;
    private final Cipher cipher;

    /**
     * Creates an instance.
     * @param key the raw key used for the AES/GCM operation
     * @param random the source of randomness used to generate AES/GCM initialization vectors
     */
    public EncryptStringFunction(byte[] key, Random random) {
        this.key = key;
        this.random = random;
        try {
            cipher = Cipher.getInstance("AES/GCM/NoPadding");
        }
        catch (NoSuchAlgorithmException | NoSuchPaddingException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    @SuppressFBWarnings(value = "PREDICTABLE_RANDOM", justification = "The PRNG is deliberately injected rather than SecureRandom, "
            + "so that masking can eventually be made to have repeatable-read semantics (e.g. seeded from topic/partition/offset)")
    public String apply(String value) {
        try {
            byte[] iv = new byte[IV_LENGTH];
            random.nextBytes(iv);
            cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(key, "AES"), new GCMParameterSpec(96, iv));
            byte[] plaintext = value.getBytes(StandardCharsets.UTF_8);
            int ciphertextSize = cipher.getOutputSize(plaintext.length);
            byte[] output = new byte[ciphertextSize + IV_LENGTH];
            int i = cipher.doFinal(plaintext, 0, plaintext.length, output);
            if (i != ciphertextSize) {
                throw new RuntimeException("Invalid");
            }
            System.arraycopy(iv, 0, output, ciphertextSize, IV_LENGTH);
            return Base64.getEncoder().encodeToString(output);
        }
        catch (IllegalBlockSizeException | BadPaddingException | InvalidKeyException | InvalidAlgorithmParameterException | ShortBufferException e) {
            throw new RuntimeException(e);
        }
    }
}

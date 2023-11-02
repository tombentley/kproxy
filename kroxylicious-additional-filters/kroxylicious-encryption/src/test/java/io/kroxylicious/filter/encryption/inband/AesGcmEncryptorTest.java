/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.encryption.inband;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

import javax.crypto.AEADBadTagException;
import javax.crypto.KeyGenerator;
import javax.crypto.ShortBufferException;
import javax.crypto.spec.SecretKeySpec;

import org.junit.jupiter.api.Test;

import io.kroxylicious.filter.encryption.EncryptionException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AesGcmEncryptorTest {

    @Test
    void shouldRoundTrip() throws NoSuchAlgorithmException {
        var keygen = KeyGenerator.getInstance("AES");
        var enc = new AesGcmEncryptor(new AesGcmIvGenerator(new SecureRandom()), keygen.generateKey());
        var plaintext = "hello, world!";
        byte[] plaintextBytes = plaintext.getBytes(StandardCharsets.UTF_8);
        var size = enc.outputSize(plaintextBytes.length);
        var ciphertext = ByteBuffer.allocate(size);
        enc.encrypt(ByteBuffer.wrap(plaintextBytes), ciphertext);
        assertFalse(ciphertext.hasRemaining());
        ciphertext.flip();
        var roundTripped = ByteBuffer.allocate(plaintextBytes.length);
        enc.decrypt(ciphertext, roundTripped);
        assertEquals(plaintext, new String(roundTripped.array(), StandardCharsets.UTF_8));
    }

    @Test
    void shouldThrowOnShortBuffer() throws NoSuchAlgorithmException {
        var keygen = KeyGenerator.getInstance("AES");
        var enc = new AesGcmEncryptor(new AesGcmIvGenerator(new SecureRandom()), keygen.generateKey());
        var plaintext = "hello, world!";
        byte[] plaintextBytes = plaintext.getBytes(StandardCharsets.UTF_8);
        var size = enc.outputSize(plaintextBytes.length);
        var ciphertext = ByteBuffer.allocate(size - 1);
        ByteBuffer wrap = ByteBuffer.wrap(plaintextBytes);
        var e = assertThrows(EncryptionException.class, () -> enc.encrypt(wrap, ciphertext));
        assertInstanceOf(ShortBufferException.class, e.getCause());
    }

    @Test
    void shouldThrowOnBadKey() {
        var badKey = new SecretKeySpec(new byte[3], "AES");
        var enc = new AesGcmEncryptor(new AesGcmIvGenerator(new SecureRandom()), badKey);
        var plaintext = "hello, world!";
        byte[] plaintextBytes = plaintext.getBytes(StandardCharsets.UTF_8);
        var e = assertThrows(EncryptionException.class, () -> enc.outputSize(plaintextBytes.length));
        assertInstanceOf(InvalidKeyException.class, e.getCause());
    }

    @Test
    void shouldThrowDeserializingUnexpectedIvLength() throws NoSuchAlgorithmException {
        var keygen = KeyGenerator.getInstance("AES");
        var enc = new AesGcmEncryptor(new AesGcmIvGenerator(new SecureRandom()), keygen.generateKey());

        ByteBuffer encoded = ByteBuffer.wrap(new byte[]{ 0, (byte) 1 });
        ByteBuffer output = ByteBuffer.allocate(10);
        var e = assertThrows(EncryptionException.class, () -> enc.decrypt(encoded, output));
        assertEquals("Unexpected IV length", e.getMessage());
    }

    @Test
    void shouldDetectChangedCiphertext() throws NoSuchAlgorithmException {
        var keygen = KeyGenerator.getInstance("AES");
        var enc = new AesGcmEncryptor(new AesGcmIvGenerator(new SecureRandom()), keygen.generateKey());
        var plaintext = "hello, world!";
        byte[] plaintextBytes = plaintext.getBytes(StandardCharsets.UTF_8);
        var size = enc.outputSize(plaintextBytes.length);
        var ciphertext = ByteBuffer.allocate(size);
        enc.encrypt(ByteBuffer.wrap(plaintextBytes), ciphertext);
        assertFalse(ciphertext.hasRemaining());
        int pos = ciphertext.position() - 1;
        ciphertext.put(pos, (byte) (ciphertext.get(pos) + 1));
        ciphertext.flip();
        var roundTripped = ByteBuffer.allocate(plaintextBytes.length);

        var e = assertThrows(EncryptionException.class, () -> enc.decrypt(ciphertext, roundTripped));
        assertInstanceOf(AEADBadTagException.class, e.getCause());

    }

    @Test
    void shouldThrowOnUnknownVersion() throws NoSuchAlgorithmException {
        var keygen = KeyGenerator.getInstance("AES");
        var enc = new AesGcmEncryptor(new AesGcmIvGenerator(new SecureRandom()), keygen.generateKey());
        var plaintext = "hello, world!";
        byte[] plaintextBytes = plaintext.getBytes(StandardCharsets.UTF_8);
        var size = enc.outputSize(plaintextBytes.length);
        var ciphertext = ByteBuffer.allocate(size);
        enc.encrypt(ByteBuffer.wrap(plaintextBytes), ciphertext);
        assertFalse(ciphertext.hasRemaining());
        ciphertext.put(0, Byte.MAX_VALUE);
        ciphertext.flip();
        var roundTripped = ByteBuffer.allocate(plaintextBytes.length);

        var e = assertThrows(EncryptionException.class, () -> enc.decrypt(ciphertext, roundTripped));
        assertEquals("Unknown version 127", e.getMessage());
    }

}

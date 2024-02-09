/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.encryption.dek;

import java.nio.ByteBuffer;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.AlgorithmParameterSpec;
import java.util.function.Supplier;

import javax.crypto.Cipher;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.IvParameterSpec;

/**
 * A CipherSpec couples a single persisted identifier with a Cipher (e.g. AES)
 * and means of generating, writing and reading parameters for that cipher.
 *
 * <h2 id="persistentIds">Persistent ids</h2>
 * {@link #persistentId}s must uniquely and immutably identify a specific CipherSpec instance.
 * They get written in the wrapper, so in order to provide backwards compatibility the ids
 * can't be removed, or associated with a different CipherSpec.
 */
public enum CipherSpec {

    /**
     * AES/GCM with 128-bit key, 96-bit IV and 128-bit tag.
     * @see <a href="https://www.ietf.org/rfc/rfc5116.txt">RFC-5116</a>
     */
    AES_128_GCM_128((byte) 0,
            "AES/GCM/NoPadding",
            1L << 32 // 2^32
    ) {

        private static final int IV_SIZE_BYTES = 12;
        private static final int TAG_LENGTH_BITS = 128;

        @Override
        Supplier<AlgorithmParameterSpec> paramSupplier() {
            var generator = new Wrapping96BitCounter(new SecureRandom());
            var iv = new byte[IV_SIZE_BYTES];
            return () -> {
                generator.generateIv(iv);
                return new GCMParameterSpec(TAG_LENGTH_BITS, iv);
            };
        }

        @Override
        public int constantParamsSize() {
            return IV_SIZE_BYTES;
        }

        @Override
        int size(AlgorithmParameterSpec parameterSpec) {
            return constantParamsSize();
        }

        @Override
        void writeParameters(
                             ByteBuffer parametersBuffer,
                             AlgorithmParameterSpec params) {
            parametersBuffer.put(((GCMParameterSpec) params).getIV());
        }

        @Override
        GCMParameterSpec readParameters(ByteBuffer parametersBuffer) {
            byte[] b = new byte[IV_SIZE_BYTES];
            parametersBuffer.get(b);
            return new GCMParameterSpec(TAG_LENGTH_BITS, b);
        }
    },
    /**
     * ChaCha20-Poly1305, which means 256-bit key, 96-bit nonce and 128-bit tag.
     * @see <a href="https://www.ietf.org/rfc/rfc7539.txt">RFC-7539</a>
     */
    CHACHA20_POLY1305((byte) 1,
            "ChaCha20-Poly1305",
            Long.MAX_VALUE // 2^96 would be necessary given we use Wrapping96BitCounter
    // 2^63-1 is sufficient
    ) {
        private static final int NONCE_SIZE_BYTES = 12;

        @Override
        Supplier<AlgorithmParameterSpec> paramSupplier() {
            // Per https://www.rfc-editor.org/rfc/rfc7539#section-4
            // we generate the nonce using a counter
            var generator = new Wrapping96BitCounter(new SecureRandom());
            var nonce = new byte[NONCE_SIZE_BYTES];
            return () -> {
                generator.generateIv(nonce);
                return new IvParameterSpec(nonce);
            };
        }

        @Override
        public int constantParamsSize() {
            return NONCE_SIZE_BYTES;
        }

        @Override
        int size(AlgorithmParameterSpec parameterSpec) {
            return constantParamsSize();
        }

        @Override
        void writeParameters(
                             ByteBuffer parametersBuffer,
                             AlgorithmParameterSpec params) {
            parametersBuffer.put(((IvParameterSpec) params).getIV());
        }

        @Override
        AlgorithmParameterSpec readParameters(ByteBuffer parametersBuffer) {
            byte[] nonce = new byte[NONCE_SIZE_BYTES];
            parametersBuffer.get(nonce);
            return new IvParameterSpec(nonce);
        }
    }
    /* !! Read the class JavaDoc before adding a new CipherSpec !! */
    ;

    /** Get the cipherSpec instance for the given <a href="#persistentIds">persistent id</a>. */
    public static CipherSpec fromPersistentId(int persistentId) {
        switch (persistentId) {
            case 0:
                return CipherSpec.AES_128_GCM_128;
            case 1:
                return CipherSpec.CHACHA20_POLY1305;
            default:
                throw new UnknownCipherSpecException("Cipher spec with persistent id " + persistentId + " is not known");
        }
    }

    private final byte persistentId;
    private final String transformation;

    private final long maxEncryptionsPerKey;

    CipherSpec(byte persistentId, String transformation, long maxEncryptionsPerKey) {
        this.persistentId = persistentId;
        this.transformation = transformation;
        this.maxEncryptionsPerKey = maxEncryptionsPerKey;
    }

    /** Get this cipherSpec's <a href="#persistentIds">persistent id</a>. */
    public byte persistentId() {
        return persistentId;
    }

    public long maxEncryptionsPerKey() {
        return maxEncryptionsPerKey;
    }

    Cipher newCipher() {
        try {
            return Cipher.getInstance(transformation);
        }
        catch (NoSuchAlgorithmException | NoSuchPaddingException e) {
            throw new DekException(e);
        }
    }

    /**
     * Return a supplier of parameters for use with the cipher.
     * The supplier need not be thread-safe.
     */
    abstract Supplier<AlgorithmParameterSpec> paramSupplier();

    /**
     * If the number of bytes required by {@link #writeParameters(ByteBuffer, AlgorithmParameterSpec)}
     * does not depend on the parameters, then returns the number.
     * Otherwise, if the number of bytes required by  {@link #writeParameters(ByteBuffer, AlgorithmParameterSpec)} is variable
     * returns {@link #VARIABLE_SIZE_PARAMETERS}.
     */
    public abstract int constantParamsSize();

    /**
     * Return the number of bytes required by {@link #writeParameters(ByteBuffer, AlgorithmParameterSpec)}
     * to serialize the given parameters.
     * If {@link #constantParamsSize()} returns a number >= 0 then this must return the same number.
     */
    abstract int size(AlgorithmParameterSpec parameterSpec);

    /**
     * Serialize the given parameters to the given buffer, which should have at least
     * {@link #size(AlgorithmParameterSpec)} bytes {@linkplain ByteBuffer#remaining() remaining}.
     */
    abstract void writeParameters(ByteBuffer parametersBuffer, AlgorithmParameterSpec params);

    /**
     * Read previously-serialize parameters from the given buffer.
     * The implementation should know how many bytes to read, so the number of
     * {@linkplain ByteBuffer#remaining() remaining} bytes need only be ≥ (not =)
     * the {@link #size(AlgorithmParameterSpec)} at the time the buffer was written.
     */
    abstract AlgorithmParameterSpec readParameters(ByteBuffer parametersBuffer);

    public static final int VARIABLE_SIZE_PARAMETERS = -1;
}
